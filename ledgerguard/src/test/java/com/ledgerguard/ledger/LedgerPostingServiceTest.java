package com.ledgerguard.ledger;
import com.ledgerguard.ledger.entity.*;
import com.ledgerguard.ledger.posting.*;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import com.ledgerguard.ledger.repository.WalletLedgerEntryRepository;
import com.ledgerguard.ledger.repository.WalletRepository;
import com.ledgerguard.ledger.state.TransactionStateService;
import com.ledgerguard.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("test")
class LedgerPostingServiceTest {
    @Autowired private LedgerPostingService postingService;
    @Autowired private TransactionStateService stateService;
    @Autowired private WalletRepository walletRepository;
    @Autowired private LedgerTransactionRepository transactionRepository;
    @Autowired private WalletLedgerEntryRepository entryRepository;
    @Autowired private TestFailureInjector failureInjector;
    @Autowired private LedgerInvariants invariants;
    @Autowired private JdbcTemplate jdbcTemplate;
    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("alpha");
        failureInjector.clear();
    }
    @AfterEach
    void tearDown() {
        invariants.assertAll("alpha");
        TenantContext.clear();
    }
    private String seedWallet(BigDecimal initialBalance, String currency) {
        String id = "w-" + UUID.randomUUID().toString().substring(0, 12);
        walletRepository.saveAndFlush(new Wallet(id, "user-1", currency, initialBalance, false));
        return id;
    }
    private String insertPendingTx(TransactionType type, String walletId, BigDecimal amount, String currency) {
        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 12);
        LedgerTransaction tx = new LedgerTransaction();
        tx.setTransactionId(txId);
        tx.setRequestHash("c".repeat(64));
        tx.setType(type);
        tx.setWalletId(walletId);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        stateService.insertPending(tx);
        return txId;
    }
    @Test
    void successfulCharge_updatesBothWalletsAndWritesTwoEntries() {
        String walletId = seedWallet(new BigDecimal("1000.00"), "INR");
        String txId = insertPendingTx(TransactionType.CHARGE, walletId, new BigDecimal("250.00"), "INR");
        PostingResult result = postingService.post(
                new PostingCommand(txId, TransactionType.CHARGE, walletId, new BigDecimal("250.00"), "INR"));
        assertEquals(TransactionStatus.SUCCESS, result.status());
        assertEquals(new BigDecimal("750.00"), result.balanceAfter());
        assertEquals(2, entryRepository.findByTransactionId(txId).size());
    }
    @Test
    void insufficientFunds_marksFailedAndWritesNoEntries() {
        String walletId = seedWallet(new BigDecimal("100.00"), "INR");
        String txId = insertPendingTx(TransactionType.CHARGE, walletId, new BigDecimal("250.00"), "INR");
        PostingResult result = postingService.post(
                new PostingCommand(txId, TransactionType.CHARGE, walletId, new BigDecimal("250.00"), "INR"));
        assertEquals(TransactionStatus.FAILED, result.status());
        assertEquals("INSUFFICIENT_FUNDS", result.failureCode());
        assertEquals(new BigDecimal("100.00"), walletRepository.findById(walletId).orElseThrow().getBalance());
        assertTrue(entryRepository.findByTransactionId(txId).isEmpty());
    }
    @Test
    void forcedFailureAfterDebit_rollsBackBalancesAndMarksRolledBack() {
        String walletId = seedWallet(new BigDecimal("500.00"), "INR");
        String txId = insertPendingTx(TransactionType.CHARGE, walletId, new BigDecimal("200.00"), "INR");
        failureInjector.failOnceAt(FailureInjector.Point.AFTER_DEBIT);
        PostingResult result = postingService.post(
                new PostingCommand(txId, TransactionType.CHARGE, walletId, new BigDecimal("200.00"), "INR"));
        assertEquals(TransactionStatus.ROLLED_BACK, result.status());
        assertEquals("SYSTEM_ERROR", result.failureCode());
        assertEquals(new BigDecimal("500.00"), walletRepository.findById(walletId).orElseThrow().getBalance());
        assertTrue(entryRepository.findByTransactionId(txId).isEmpty());
    }
    @Test
    void concurrentCharges_20ThreadsOn100Balance_exactly10Succeed() throws Exception {
        String walletId = seedWallet(new BigDecimal("100.00"), "INR");
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        for (int i = 0; i < threads; i++) {
            String txId = insertPendingTx(TransactionType.CHARGE, walletId, new BigDecimal("10.00"), "INR");
            pool.submit(() -> {
                TenantContext.setTenantId("alpha");
                ready.countDown();
                try {
                    go.await();
                    PostingResult res = postingService.post(
                            new PostingCommand(txId, TransactionType.CHARGE, walletId, new BigDecimal("10.00"), "INR"));
                    if (res.status() == TransactionStatus.SUCCESS) successCount.incrementAndGet();
                    else if (res.status() == TransactionStatus.FAILED) failedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    TenantContext.clear();
                }
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        while (!pool.isTerminated()) Thread.sleep(15);
        assertEquals(10, successCount.get());
        assertEquals(10, failedCount.get());
        assertEquals(new BigDecimal("0.00"), walletRepository.findById(walletId).orElseThrow().getBalance());
    }
    @Test
    void decimalPrecision_tenChargesOfZeroTen_equalsExactOne() {
        String walletId = seedWallet(new BigDecimal("1.00"), "INR");
        for (int i = 0; i < 10; i++) {
            String txId = insertPendingTx(TransactionType.CHARGE, walletId, new BigDecimal("0.10"), "INR");
            PostingResult res = postingService.post(
                    new PostingCommand(txId, TransactionType.CHARGE, walletId, new BigDecimal("0.10"), "INR"));
            assertEquals(TransactionStatus.SUCCESS, res.status());
        }
        assertEquals(new BigDecimal("0.00"), walletRepository.findById(walletId).orElseThrow().getBalance());
    }
    @Test
    void checkConstraint_rejectsNegativeCustomerWalletBalance() {
        String walletId = seedWallet(new BigDecimal("50.00"), "INR");
        assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update("UPDATE billing_wallets SET balance = -5.00 WHERE id = ?", walletId));
    }
}
