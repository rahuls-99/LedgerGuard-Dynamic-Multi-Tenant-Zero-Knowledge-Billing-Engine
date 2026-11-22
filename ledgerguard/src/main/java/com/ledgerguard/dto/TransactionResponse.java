package com.ledgerguard.dto;

import java.math.BigDecimal;

public class TransactionResponse {

    private String transactionId;
    private String organizationId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private boolean replayed;

    public TransactionResponse() {}

    public TransactionResponse(String transactionId, String organizationId, BigDecimal amount,
                               String currency, String status, boolean replayed) {
        this.transactionId = transactionId;
        this.organizationId = organizationId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.replayed = replayed;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isReplayed() { return replayed; }
    public void setReplayed(boolean replayed) { this.replayed = replayed; }
}