package com.ledgerguard.entity;

public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    ROLLED_BACK;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == ROLLED_BACK;
    }

    public boolean canTransitionTo(TransactionStatus target) {
        if (this.isTerminal() || target == null) {
            return false;
        }
        return this == PENDING && target.isTerminal();
    }
}