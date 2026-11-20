package com.ledgerguard.ledger.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity(name = "BillingWalletLedgerEntry")
@Table(name = "billing_ledger_entries")
public class WalletLedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "entry_id", length = 40, nullable = false, unique = true)
    private String entryId;
    @Column(name = "transaction_id", length = 40, nullable = false)
    private String transactionId;
    @Column(name = "wallet_id", length = 36, nullable = false)
    private String walletId;
    @Enumerated(EnumType.STRING)
    @Column(length = 6, nullable = false)
    private EntryDirection direction;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;
    @Column(name = "balance_after", precision = 19, scale = 2, nullable = false)
    private BigDecimal balanceAfter;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    public WalletLedgerEntry() {}
    public WalletLedgerEntry(String entryId, String transactionId, String walletId,
                             EntryDirection direction, BigDecimal amount, BigDecimal balanceAfter) {
        this.entryId = entryId; this.transactionId = transactionId; this.walletId = walletId;
        this.direction = direction; this.amount = amount; this.balanceAfter = balanceAfter;
        this.createdAt = Instant.now();
    }
    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
    public Long getId() { return id; }
    public String getEntryId() { return entryId; }
    public String getTransactionId() { return transactionId; }
    public String getWalletId() { return walletId; }
    public EntryDirection getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Instant getCreatedAt() { return createdAt; }
}
