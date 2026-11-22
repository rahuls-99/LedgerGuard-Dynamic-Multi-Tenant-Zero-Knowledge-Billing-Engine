package com.ledgerguard.ledger.api;

import java.time.Duration;

public final class RequestBudget {

    private final long deadlineNanos;

    private RequestBudget(Duration totalBudget) {
        long nanos = (totalBudget != null && !totalBudget.isNegative() && !totalBudget.isZero())
                ? totalBudget.toNanos()
                : Duration.ofSeconds(25).toNanos();
        this.deadlineNanos = System.nanoTime() + nanos;
    }

    public static RequestBudget start(Duration totalBudget) {
        return new RequestBudget(totalBudget);
    }

    public static RequestBudget default25s() {
        return new RequestBudget(Duration.ofSeconds(25));
    }

    public Duration remaining() {
        long remNanos = deadlineNanos - System.nanoTime();
        return remNanos > 0 ? Duration.ofNanos(remNanos) : Duration.ZERO;
    }

    public boolean hasAtLeast(Duration required) {
        if (required == null || required.isZero() || required.isNegative()) {
            return remaining().toNanos() > 0;
        }
        return remaining().compareTo(required) >= 0;
    }

    public void requireAtLeast(Duration required) {
        if (!hasAtLeast(required)) {
            throw new BudgetExhaustedException(
                    "Insufficient request budget remaining: " + remaining().toMillis()
                            + "ms, required: " + required.toMillis() + "ms"
            );
        }
    }

    public static final class BudgetExhaustedException extends RuntimeException {
        public BudgetExhaustedException(String message) {
            super(message);
        }
    }
}