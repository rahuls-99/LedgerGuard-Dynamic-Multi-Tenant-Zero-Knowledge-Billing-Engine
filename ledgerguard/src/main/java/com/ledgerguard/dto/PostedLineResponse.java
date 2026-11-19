package com.ledgerguard.dto;

import java.math.BigDecimal;

public class PostedLineResponse {
    private String entryId;
    private String accountId;
    private String direction;
    private BigDecimal amount;
    private BigDecimal updatedBalance;

    public PostedLineResponse(String entryId, String accountId, String direction, BigDecimal amount, BigDecimal updatedBalance) {
        this.entryId = entryId;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
        this.updatedBalance = updatedBalance;
    }

    public String getEntryId() { return entryId; }
    public String getAccountId() { return accountId; }
    public String getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getUpdatedBalance() { return updatedBalance; }
}