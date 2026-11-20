package com.ledgerguard.ledger.entity;
public enum TransactionStatus {
    PENDING, SUCCESS, FAILED, ROLLED_BACK;
    public boolean isTerminal() { return this == SUCCESS || this == FAILED || this == ROLLED_BACK; }
    public boolean canTransitionTo(TransactionStatus target) { return !isTerminal() && target != null && target.isTerminal(); }
}
