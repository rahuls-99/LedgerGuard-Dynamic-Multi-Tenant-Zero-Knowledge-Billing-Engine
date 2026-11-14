package com.ledgerguard.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_org", columnList = "orgId")
})
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64)
    private String orgId;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    public Invoice() {
    }
    public Invoice(String orgId, String customerName, BigDecimal amount, String status) {
        this.orgId = orgId;
        this.customerName = customerName;
        this.amount = amount;
        this.status = status;
        this.createdAt = Instant.now();
    }
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}