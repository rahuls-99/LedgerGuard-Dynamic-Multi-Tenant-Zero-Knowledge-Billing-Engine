package com.ledgerguard.ledger.state;
public enum FailureCode {
    INSUFFICIENT_FUNDS("Insufficient balance. Nothing was charged."),
    CURRENCY_MISMATCH("Currency does not match the wallet. Nothing was charged."),
    SYSTEM_ERROR("A system error occurred and the transaction was reversed. Nothing was charged."),
    DEADLOCK_RETRIES_EXHAUSTED("The system was busy and the transaction was reversed. Nothing was charged."),
    TIMEOUT("The transaction took too long and was reversed. Nothing was charged."),
    STUCK_PENDING_RECOVERED("The transaction was interrupted and reversed. Nothing was charged.");
    private final String userMessage;
    FailureCode(String userMessage) { this.userMessage = userMessage; }
    public String getUserMessage() { return userMessage; }
}
