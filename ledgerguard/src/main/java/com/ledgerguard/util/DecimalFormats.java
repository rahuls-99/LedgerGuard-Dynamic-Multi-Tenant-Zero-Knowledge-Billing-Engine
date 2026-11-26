package com.ledgerguard.analytics.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DecimalFormats {

    private static final int MONEY_SCALE = 2;
    private static final int QTY_SCALE = 4;

    private DecimalFormats() {
    }

    public static BigDecimal roundMoney(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal roundQty(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(QTY_SCALE, RoundingMode.HALF_UP);
    }

    public static String fmt2(BigDecimal value) {
        return roundMoney(value).toPlainString();
    }

    public static String fmt4(BigDecimal value) {
        return roundQty(value).toPlainString();
    }
}