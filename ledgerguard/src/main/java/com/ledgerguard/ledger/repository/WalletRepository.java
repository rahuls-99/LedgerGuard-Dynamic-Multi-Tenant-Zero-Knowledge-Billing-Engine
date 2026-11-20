package com.ledgerguard.ledger.repository;
import com.ledgerguard.ledger.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository("billingWalletRepository")
public interface WalletRepository extends JpaRepository<Wallet, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM BillingWallet w WHERE w.id IN :ids ORDER BY w.id ASC")
    List<Wallet> findAllByIdInOrderByIdAscForUpdate(@Param("ids") List<String> ids);
    Optional<Wallet> findFirstBySystemTrueAndCurrency(String currency);
    @Query("SELECT w FROM BillingWallet w WHERE w.system = true AND w.currency = :currency AND w.id LIKE :prefix%")
    Optional<Wallet> findSystemWalletByCurrencyAndPrefix(@Param("currency") String currency, @Param("prefix") String prefix);
}