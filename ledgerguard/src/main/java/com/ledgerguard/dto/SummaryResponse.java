package com.ledgerguard.analytics.dto;

import java.util.List;

public record SummaryResponse(
        String tenantId,
        String baseCurrency,
        String from,
        String to,
        PeriodSummary current,
        PeriodSummary previous,
        ChangeSummary changes,
        UsageSummary usage
) {
    public record PeriodSummary(
            long total,
            long success,
            long failed,
            long rolledBack,
            long otherCurrencyCount,
            String successRate,
            String amountCharged,
            String amountToppedUp,
            String amountRefunded
    ) {}

    public record ChangeSummary(
            String totalPercent,
            String successRatePercent,
            String amountChargedPercent,
            String totalCostPercent
    ) {}

    public record UsageSummary(
            String totalCost,
            String previousTotalCost,
            List<ResourceUsageItem> byResource
    ) {}

    public record ResourceUsageItem(
            String code,
            String name,
            String unit,
            String quantity,
            String cost
    ) {}
}