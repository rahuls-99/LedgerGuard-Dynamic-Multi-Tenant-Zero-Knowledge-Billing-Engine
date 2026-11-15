package com.ledgerguard.seed;

import com.ledgerguard.entity.*;
import com.ledgerguard.repository.LedgerEntryRepository;
import com.ledgerguard.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class LedgerDataSeeder implements CommandLineRunner {

    private final TransactionRepository txRepo;
    private final LedgerEntryRepository entryRepo;

    public LedgerDataSeeder(TransactionRepository txRepo, LedgerEntryRepository entryRepo) {
        this.txRepo = txRepo;
        this.entryRepo = entryRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // orgA: 5 transactions
        seedTxn("orgA", "TXN-SEED-A01", "idem-a-01", TransactionType.TOPUP, "w_orga_1", "5000.00", TransactionStatus.SUCCESS, "5000.00");
        seedTxn("orgA", "TXN-SEED-A02", "idem-a-02", TransactionType.CHARGE, "w_orga_1", "1200.00", TransactionStatus.SUCCESS, "3800.00");
        seedTxn("orgA", "TXN-SEED-A03", "idem-a-03", TransactionType.REFUND, "w_orga_1", "200.00", TransactionStatus.ROLLED_BACK, "3800.00");
        seedTxn("orgA", "TXN-SEED-A04", null, TransactionType.CHARGE, "w_orga_1", "950.00", TransactionStatus.PENDING, null);
        seedTxn("orgA", "TXN-SEED-A05", null, TransactionType.CHARGE, "w_orga_1", "99000.00", TransactionStatus.FAILED, null);

        // orgB: 3 transactions
        seedTxn("orgB", "TXN-SEED-B01", "idem-b-01", TransactionType.TOPUP, "w_orgb_1", "8000.00", TransactionStatus.SUCCESS, "8000.00");
        seedTxn("orgB", "TXN-SEED-B02", "idem-b-02", TransactionType.CHARGE, "w_orgb_1", "1500.00", TransactionStatus.SUCCESS, "6500.00");
        seedTxn("orgB", "TXN-SEED-B03", null, TransactionType.CHARGE, "w_orgb_1", "400.00", TransactionStatus.PENDING, null);
    }

    private void seedTxn(String tenantId, String txnId, String idemKey, TransactionType type,
                         String walletId, String amountStr, TransactionStatus status, String balanceAfterStr) {
        if (txRepo.existsByTransactionId(txnId)) {
            return; // Idempotent guard
        }

        Transaction tx = new Transaction();
        tx.setTenantId(tenantId);
        tx.setTransactionId(txnId);
        tx.setIdempotencyKey(idemKey);
        tx.setRequestHash("sha256-seed-" + txnId.toLowerCase());
        tx.setType(type);
        tx.setWalletId(walletId);
        tx.setAmount(new BigDecimal(amountStr));
        tx.setCurrency("INR");
        tx.setStatus(status);
        txRepo.save(tx);

        if (balanceAfterStr != null) {
            LedgerEntry entry = new LedgerEntry();
            entry.setTenantId(tenantId);
            entry.setEntryId("ENT-" + txnId);
            entry.setTransactionId(txnId);
            entry.setWalletId(walletId);
            entry.setDirection(type == TransactionType.CHARGE ? EntryDirection.DEBIT : EntryDirection.CREDIT);
            entry.setAmount(new BigDecimal(amountStr));
            entry.setBalanceAfter(new BigDecimal(balanceAfterStr));
            entryRepo.save(entry);
        }
    }
}