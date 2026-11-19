package com.ledgerguard;

import com.ledgerguard.entity.*;
import com.ledgerguard.repository.LedgerEntryRepository;
import com.ledgerguard.repository.TransactionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest
class LedgerSchemaTest {

    @Autowired
    private TransactionRepository txRepo;

    @Autowired
    private LedgerEntryRepository entryRepo;

    @Autowired
    private TransactionTemplate txTemplate;

    private Transaction buildSampleTxn(String tenantId, String txnId, String idemKey, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setTenantId(tenantId);
        tx.setTransactionId(txnId);
        tx.setIdempotencyKey(idemKey);
        tx.setRequestHash("hash-" + UUID.randomUUID());
        tx.setType(TransactionType.CHARGE);
        tx.setWalletId("w_test_1");
        tx.setAmount(amount);
        tx.setCurrency("INR");
        tx.setStatus(TransactionStatus.PENDING);
        return tx;
    }

    @Test
    void shouldRejectDuplicateTransactionId() {
        String dupId = "TXN-DUP-" + UUID.randomUUID();
        txRepo.saveAndFlush(buildSampleTxn("orgA", dupId, null, new BigDecimal("100.00")));

        Assertions.assertThrows(DataIntegrityViolationException.class, () ->
                txRepo.saveAndFlush(buildSampleTxn("orgA", dupId, null, new BigDecimal("200.00")))
        );
    }

    @Test
    void shouldRejectDuplicateIdempotencyKeyWithinSameTenant() {
        String idem = "idem-" + UUID.randomUUID();
        txRepo.saveAndFlush(buildSampleTxn("orgA", "TXN-" + UUID.randomUUID(), idem, new BigDecimal("100.00")));

        Assertions.assertThrows(DataIntegrityViolationException.class, () ->
                txRepo.saveAndFlush(buildSampleTxn("orgA", "TXN-" + UUID.randomUUID(), idem, new BigDecimal("100.00")))
        );
    }

    @Test
    void shouldAllowMultipleTransactionsWithNullIdempotencyKey() {
        Transaction t1 = txRepo.saveAndFlush(buildSampleTxn("orgA", "TXN-" + UUID.randomUUID(), null, new BigDecimal("50.00")));
        Transaction t2 = txRepo.saveAndFlush(buildSampleTxn("orgB", "TXN-" + UUID.randomUUID(), null, new BigDecimal("75.00")));

        Assertions.assertNotNull(t1.getId());
        Assertions.assertNotNull(t2.getId());
    }

    @Test
    void shouldRejectZeroOrNegativeAmount() {
        Assertions.assertThrows(Exception.class, () ->
                txRepo.saveAndFlush(buildSampleTxn("orgA", "TXN-" + UUID.randomUUID(), null, BigDecimal.ZERO))
        );
        Assertions.assertThrows(Exception.class, () ->
                txRepo.saveAndFlush(buildSampleTxn("orgA", "TXN-" + UUID.randomUUID(), null, new BigDecimal("-10.00")))
        );
    }

    @Test
    void shouldEnforceValidStatusTransitions() {
        Transaction tx = buildSampleTxn("orgA", "TXN-" + UUID.randomUUID(), null, new BigDecimal("100.00"));
        tx.transitionTo(TransactionStatus.SUCCESS, null);
        Assertions.assertEquals(TransactionStatus.SUCCESS, tx.getStatus());

        // SUCCESS -> PENDING is invalid
        Assertions.assertThrows(IllegalStateException.class, () -> tx.transitionTo(TransactionStatus.PENDING, null));

        // SUCCESS -> ROLLED_BACK is valid
        tx.transitionTo(TransactionStatus.ROLLED_BACK, "User dispute");
        Assertions.assertEquals(TransactionStatus.ROLLED_BACK, tx.getStatus());

        // ROLLED_BACK is terminal
        Assertions.assertThrows(IllegalStateException.class, () -> tx.transitionTo(TransactionStatus.SUCCESS, null));
    }

    @Test
    void shouldRollbackBothTransactionAndEntryOnMidTransactionException() {
        String txnId = "TXN-ROLLBACK-" + UUID.randomUUID();
        String walletId = "w_rb_" + UUID.randomUUID().toString().substring(0, 8);

        Assertions.assertThrows(RuntimeException.class, () ->
                txTemplate.executeWithoutResult(status -> {
                    txRepo.saveAndFlush(buildSampleTxn("orgA", txnId, null, new BigDecimal("300.00")));

                    LedgerEntry entry = new LedgerEntry();
                    entry.setTenantId("orgA");
                    entry.setEntryId("ENT-" + txnId);
                    entry.setTransactionId(txnId);
                    entry.setWalletId(walletId);
                    entry.setDirection(EntryDirection.DEBIT);
                    entry.setAmount(new BigDecimal("300.00"));
                    entry.setBalanceAfter(new BigDecimal("700.00"));
                    entryRepo.saveAndFlush(entry);

                    throw new RuntimeException("Simulated mid-transaction crash");
                })
        );

        Assertions.assertFalse(txRepo.existsByTransactionId(txnId));
        Assertions.assertTrue(entryRepo.findByTenantIdAndWalletIdOrderByCreatedAtDesc("orgA", walletId).isEmpty());
    }
}