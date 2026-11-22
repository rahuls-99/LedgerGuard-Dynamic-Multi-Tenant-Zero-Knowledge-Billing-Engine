package com.ledgerguard.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "ledger_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_txn_transaction_id", columnNames = {"transaction_id"}),
                @UniqueConstraint(name = "uk_txn_tenant_idempotency", columnNames = {"tenant_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_txn_tenant_wallet_created", columnList = "tenant_id, wallet_id, created_at"),
                @Index(name = "idx_txn_tenant_status_created", columnList = "tenant_id, status, created_at")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId; // "TXN-" + UUID

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey; // NULL when absent; MySQL UNIQUE allows multiple NULLs

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash; // SHA-256 hex

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Column(name = "wallet_id", nullable = false, length = 64)
    private String walletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        validateAmount();
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = TransactionStatus.PENDING;
    }

    @PreUpdate
    void onUpdate() {
        validateAmount();
        this.updatedAt = Instant.now();
    }

    private void validateAmount() {
        if (this.amount == null || this.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be > 0");
        }
    }

    public void transitionTo(TransactionStatus nextStatus, String reason) {
        if (!this.status.canTransitionTo(nextStatus)) {
            throw new IllegalStateException("Invalid status transition: " + this.status + " -> " + nextStatus);
        }
        this.status = nextStatus;
        if (reason != null) {
            this.failureReason = reason;
        }
    }

    // Standard Getters & Setters
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
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
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}