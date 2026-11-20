package com.ledgerguard.ledger.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity(name = "WalletLedgerTransaction")
@Table(name = "billing_transactions")
public class LedgerTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", length = 40, nullable = false, unique = true)
    private String transactionId;
    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;
    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private TransactionType type;
    @Column(name = "wallet_id", length = 36, nullable = false)
    private String walletId;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;
    @Column(length = 3, nullable = false)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private TransactionStatus status;
    @Column(name = "failure_reason", length = 255)
    private String failureReason;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
