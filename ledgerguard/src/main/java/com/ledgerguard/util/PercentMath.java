package com.ledgerguard.analytics.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class PercentMath {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int INTERMEDIATE_SCALE = 12;

    private PercentMath() {}

    public static String successRate(long success, long total) {
        if (total <= 0) {
            return "0.00";
        }
        BigDecimal rate = BigDecimal.valueOf(success)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        return rate.toPlainString();
    }

    public static String changePercent(BigDecimal current, BigDecimal previous) {
        BigDecimal cur = current == null ? BigDecimal.ZERO : current;
        BigDecimal prev = previous == null ? BigDecimal.ZERO : previous;
        if (prev.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal diff = cur.subtract(prev, MathContext.DECIMAL128);
        BigDecimal pct = diff.multiply(HUNDRED, MathContext.DECIMAL128)
                .divide(prev, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        return pct.toPlainString();
    }

    public static String changePercent(long current, long previous) {
        return changePercent(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }
}