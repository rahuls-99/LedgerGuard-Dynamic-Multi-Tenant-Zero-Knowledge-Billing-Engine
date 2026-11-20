package com.ledgerguard.ledger.repository;
import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
@Repository("walletLedgerTransactionRepository")
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {
    Optional<LedgerTransaction> findByTransactionId(String transactionId);
    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);
    @Query("SELECT t FROM WalletLedgerTransaction t WHERE t.status = 'PENDING' AND t.createdAt < :cutoff")
    List<LedgerTransaction> findStuckPendingBefore(@Param("cutoff") Instant cutoff);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WalletLedgerTransaction t SET t.status = :status, t.failureReason = :code, t.updatedAt = CURRENT_TIMESTAMP WHERE t.transactionId = :txId AND t.status = 'PENDING'")
    int transitionFromPending(@Param("txId") String txId, @Param("status") TransactionStatus status, @Param("code") String failureCode);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WalletLedgerTransaction t WHERE t.transactionId = :txId AND t.status = 'PENDING'")
    int deleteIfPending(@Param("txId") String txId);
}