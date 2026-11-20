package com.ledgerguard.ledger.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity(name = "BillingWallet")
@Table(name = "billing_wallets")
public class Wallet {
    @Id
    @Column(length = 36, nullable = false)
    private String id;
    @Column(name = "owner_id", length = 64, nullable = false)
    private String ownerId;
    @Column(length = 3, nullable = false)
    private String currency;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;
    @Column(name = "is_system", nullable = false)
    private boolean system;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    public Wallet() {}
    public Wallet(String id, String ownerId, String currency, BigDecimal balance, boolean system) {
        this.id = id; this.ownerId = ownerId; this.currency = currency;
        this.balance = balance; this.system = system; this.updatedAt = Instant.now();
    }
    @PrePersist @PreUpdate void touch() { this.updatedAt = Instant.now(); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }
    public Instant getUpdatedAt() { return updatedAt; }
}
