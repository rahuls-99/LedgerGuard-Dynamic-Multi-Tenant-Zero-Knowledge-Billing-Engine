package com.ledgerguard.analytics.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class DateRanges {

    public record RangeWindow(
            LocalDate fromInclusive,
            LocalDate toInclusive,
            long dayCount,
            LocalDate prevFromInclusive,
            LocalDate prevToInclusive,
            LocalDateTime prevFromUtc,
            LocalDateTime curFromUtc,
            LocalDateTime toExclusiveUtc
    ) {}

    private DateRanges() {}

    public static RangeWindow fromUtcDates(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid date range: from must be <= to");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);

        LocalDateTime prevFromUtc = prevFrom.atStartOfDay();
        LocalDateTime curFromUtc = from.atStartOfDay();
        LocalDateTime toExclusiveUtc = to.plusDays(1).atStartOfDay();

        return new RangeWindow(
                from,
                to,
                days,
                prevFrom,
                prevTo,
                prevFromUtc,
                curFromUtc,
                toExclusiveUtc
        );
    }

    public static LocalDate defaultUtcToday() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}