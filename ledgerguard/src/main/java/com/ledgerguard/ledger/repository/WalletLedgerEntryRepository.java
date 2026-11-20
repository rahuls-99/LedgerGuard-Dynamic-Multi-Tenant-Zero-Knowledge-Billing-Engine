package com.ledgerguard.ledger.repository;
import com.ledgerguard.ledger.entity.WalletLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository("billingWalletLedgerEntryRepository")
public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, Long> {
    List<WalletLedgerEntry> findByTransactionId(String transactionId);
    List<WalletLedgerEntry> findTop50ByWalletIdOrderByCreatedAtDesc(String walletId);
}