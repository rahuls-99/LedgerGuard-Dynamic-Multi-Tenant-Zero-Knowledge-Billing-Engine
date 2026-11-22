package com.ledgerguard.ledger.posting;

public interface FailureInjector {
    enum Point {
        AFTER_PENDING_INSERT,
        AFTER_DEBIT,
        BEFORE_COMMIT,
        AFTER_COMMIT
    }

    void checkpoint(Point point);

    default void checkpoint(String key) {
        if (key == null) return;
        try {
            checkpoint(Point.valueOf(key.trim().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            // Non-enum custom key handled by implementation if supported
        }
    }
}