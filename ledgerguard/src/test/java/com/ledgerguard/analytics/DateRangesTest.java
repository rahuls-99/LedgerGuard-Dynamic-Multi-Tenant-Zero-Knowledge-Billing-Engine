package com.ledgerguard.analytics;

import com.ledgerguard.analytics.util.DateRanges;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateRangesTest {

    @Test
    void buildsHalfOpenUtcAndPreviousPeriod() {
        DateRanges.RangeWindow win = DateRanges.fromUtcDates(
                LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 11, 20)
        );
        assertEquals(30L, win.dayCount());
        assertEquals(LocalDateTime.of(2026, 10, 22, 0, 0), win.curFromUtc());
        assertEquals(LocalDateTime.of(2026, 11, 21, 0, 0), win.toExclusiveUtc());
        assertEquals(LocalDate.of(2026, 9, 22), win.prevFromInclusive());
        assertEquals(LocalDate.of(2026, 10, 21), win.prevToInclusive());
    }
}