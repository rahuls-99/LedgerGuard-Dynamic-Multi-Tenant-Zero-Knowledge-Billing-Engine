package com.ledgerguard.dto;

import java.util.List;

public record SummaryResponse(
        String tenantId,
        String baseCurrency,
        String granularity,
        WindowRange currentWindow,
        WindowRange previousWindow,
        PeriodSummary current,
        PeriodSummary previous,
        ChangeSummary change,
        UsageSummary usage
) {
    public record WindowRange(
            String from,
            String to
    ) {}

    public record PeriodSummary(
            long total,
            long success,
            long failed,
            long rolledBack,
            long otherCurrencyCount,
            String successRate,
            String charged,
            String toppedUp,
            String refunded
    ) {}

    public record ChangeSummary(
            String totalDeltaPct,
            String chargedDeltaPct,
            String successRateDeltaPct
    ) {}

    public record UsageSummary(
            String totalUsageAmount,
            List<ResourceUsageItem> items
    ) {}

    public record ResourceUsageItem(
            String code,
            String name,
            String unit,
            long totalQuantity,
            String billedAmount,
            String unbilledAmount
    ) {}
}