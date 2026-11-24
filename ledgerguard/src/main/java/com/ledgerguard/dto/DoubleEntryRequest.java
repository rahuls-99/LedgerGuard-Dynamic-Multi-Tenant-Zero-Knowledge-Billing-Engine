package com.ledgerguard.dto;

import java.util.List;

public class DoubleEntryRequest {
    private String referenceId;
    private String currency;
    private String description;
    private List<JournalLineRequest> entries;

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<JournalLineRequest> getEntries() { return entries; }
    public void setEntries(List<JournalLineRequest> entries) { this.entries = entries; }
}