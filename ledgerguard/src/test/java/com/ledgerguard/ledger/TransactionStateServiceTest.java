package com.ledgerguard.ledger;
import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.entity.TransactionType;
import com.ledgerguard.ledger.posting.CurrencyMismatchException;
import com.ledgerguard.ledger.posting.InsufficientFundsException;
import com.ledgerguard.ledger.entity.Wallet;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import com.ledgerguard.ledger.repository.WalletRepository;
import com.ledgerguard.ledger.state.FailureClassifier;
import com.ledgerguard.ledger.state.FailureCode;
import com.ledgerguard.ledger.state.TransactionStateService;
import com.ledgerguard.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.QueryTimeoutException;
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
class TransactionStateServiceTest {
    @Autowired private TransactionStateService stateService;
    @Autowired private LedgerTransactionRepository transactionRepository;
    @Autowired private FailureClassifier classifier;
    @Autowired private WalletRepository walletRepository;
    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("alpha");
        if (!walletRepository.existsById("w-state-test")) { walletRepository.saveAndFlush(new Wallet("w-state-test", "u1", "INR", new BigDecimal("100.00"), false)); }
    }
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }
    @Test
    void twoThreadsFinalizingSameTransaction_onlyOneWins() throws Exception {
        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 12);
        LedgerTransaction tx = new LedgerTransaction();
        tx.setTransactionId(txId);
        tx.setRequestHash("a".repeat(64));
        tx.setType(TransactionType.CHARGE);
        tx.setWalletId("w-state-test");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setCurrency("INR");
        stateService.insertPending(tx);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger(0);
        for (int i = 0; i < 2; i++) {
            final boolean fail = (i == 0);
            pool.submit(() -> {
                TenantContext.setTenantId("alpha");
        if (!walletRepository.existsById("w-state-test")) { walletRepository.saveAndFlush(new Wallet("w-state-test", "u1", "INR", new BigDecimal("100.00"), false)); }
                ready.countDown();
                try {
                    go.await();
                    boolean won = fail
                            ? stateService.markFailed(txId, FailureCode.INSUFFICIENT_FUNDS)
                            : stateService.markRolledBack(txId, FailureCode.SYSTEM_ERROR);
                    if (won) wins.incrementAndGet();
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
        while (!pool.isTerminated()) Thread.sleep(10);
        assertEquals(1, wins.get(), "Exactly one thread must win conditional transition");
    }
    @Test
    void terminalStatesAreImmutable() {
        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 12);
        LedgerTransaction tx = new LedgerTransaction();
        tx.setTransactionId(txId);
        tx.setRequestHash("b".repeat(64));
        tx.setType(TransactionType.CHARGE);
        tx.setWalletId("w-state-test");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setCurrency("INR");
        stateService.insertPending(tx);
        assertTrue(stateService.markFailed(txId, FailureCode.INSUFFICIENT_FUNDS));
        assertFalse(stateService.markRolledBack(txId, FailureCode.SYSTEM_ERROR));
        assertFalse(stateService.discardPending(txId));
        LedgerTransaction saved = transactionRepository.findByTransactionId(txId).orElseThrow();
        assertEquals(TransactionStatus.FAILED, saved.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", saved.getFailureReason());
    }
    @Test
    void classifierMapsExceptionsToSafeCodesWithoutSqlLeak() {
        var c1 = classifier.classify(new InsufficientFundsException("raw SQL details"));
        assertEquals(TransactionStatus.FAILED, c1.status());
        assertEquals(FailureCode.INSUFFICIENT_FUNDS, c1.code());
        assertFalse(c1.code().getUserMessage().contains("SQL"));
        var c2 = classifier.classify(new CurrencyMismatchException("mismatch"));
        assertEquals(FailureCode.CURRENCY_MISMATCH, c2.code());
        var c3 = classifier.classify(new QueryTimeoutException("JDBC timeout"));
        assertEquals(TransactionStatus.ROLLED_BACK, c3.status());
        assertEquals(FailureCode.TIMEOUT, c3.code());
        assertFalse(c3.code().getUserMessage().contains("JDBC"));
    }
}
