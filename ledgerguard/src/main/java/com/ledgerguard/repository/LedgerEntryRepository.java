package com.ledgerguard.repository;

import com.ledgerguard.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTenantIdAndWalletIdOrderByCreatedAtDesc(String tenantId, String walletId);
}