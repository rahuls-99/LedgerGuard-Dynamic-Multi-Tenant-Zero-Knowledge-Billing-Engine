package com.ledgerguard.dto;

import java.math.BigDecimal;

public class JournalLineRequest {
    private String accountId;
    private String direction;
    private BigDecimal amount;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}