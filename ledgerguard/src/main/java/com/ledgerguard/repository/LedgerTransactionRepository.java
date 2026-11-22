package com.ledgerguard.repository;
import com.ledgerguard.entity.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {
    Optional<LedgerTransaction> findByTransactionId(String transactionId);
    List<LedgerTransaction> findByOrganizationId(String organizationId);
    List<LedgerTransaction> findByAccountId(String accountId);
}