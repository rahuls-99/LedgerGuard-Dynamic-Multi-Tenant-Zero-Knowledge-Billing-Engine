package com.ledgerguard.repository;

import com.ledgerguard.entity.Transaction;
import com.ledgerguard.entity.TransactionStatus;
import com.ledgerguard.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTenantIdAndTransactionId(String tenantId, String transactionId);

    Optional<Transaction> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    boolean existsByTransactionId(String transactionId);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.tenantId = :tenantId
          AND (:status IS NULL OR t.status = :status)
          AND (:type IS NULL OR t.type = :type)
          AND (:fromDate IS NULL OR t.createdAt >= :fromDate)
          AND (:toDate IS NULL OR t.createdAt <= :toDate)
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> searchTransactions(
            @Param("tenantId") String tenantId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}