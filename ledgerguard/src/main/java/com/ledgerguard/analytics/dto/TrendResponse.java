package com.ledgerguard.analytics.dto;
import java.time.LocalDate;
import java.util.List;
public record TrendResponse(
        String granularity,
        String currency,
        LocalDate from,
        LocalDate to,
        Filters filters,
        List<TrendBucket> buckets,
        TrendTotals totals,
        PeakBucket peak
) {
    public record Filters(
            List<String> types,
            long otherCurrencyCount
    ) {}
    public record TrendBucket(
            LocalDate bucketStart,
            LocalDate bucketEnd,
            boolean partial,
            long total,
            long success,
            long failed,
            long rolledBack,
            String successRate,
            String amountCharged
    ) {}
    public record TrendTotals(
            long total,
            long success,
            long failed,
            long rolledBack,
            String successRate,
            String amountCharged
    ) {}
    public record PeakBucket(
            LocalDate bucketStart,
            long total
    ) {}
}