package com.ledgerguard.entity;

public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    ROLLED_BACK;

    public boolean canTransitionTo(TransactionStatus target) {
        if (target == null || target == this) return false;
        return switch (this) {
            case PENDING -> target == SUCCESS || target == FAILED;
            case SUCCESS -> target == ROLLED_BACK;
            case FAILED, ROLLED_BACK -> false;
        };
    }
}