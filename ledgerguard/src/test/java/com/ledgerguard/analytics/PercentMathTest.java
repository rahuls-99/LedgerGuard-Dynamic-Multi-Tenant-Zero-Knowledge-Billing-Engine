package com.ledgerguard.analytics;

import com.ledgerguard.analytics.util.PercentMath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PercentMathTest {

    @Test
    void successRate_handlesZeroAndFractions() {
        assertEquals("0.00", PercentMath.successRate(0, 0));
        assertEquals("33.33", PercentMath.successRate(1, 3));
        assertEquals("66.67", PercentMath.successRate(2, 3));
    }

    @Test
    void changePercent_handlesZeroPreviousAndHalfUpAndNegative() {
        assertNull(PercentMath.changePercent(new BigDecimal("100.00"), BigDecimal.ZERO));
        assertEquals("-3.20", PercentMath.changePercent(new BigDecimal("96.80"), new BigDecimal("100.00")));
        assertEquals("-99.50", PercentMath.changePercent(new BigDecimal("0.005"), new BigDecimal("1.00")));
    }
}