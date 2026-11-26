package com.ledgerguard.analytics.dto;

import java.util.List;

public record UsageResponse(
        RangeInfo range,
        String granularity,
        String currency,
        List<ResourceCatalogItem> resources,
        List<ResourceSeries> series,
        List<String> totalCostSeries,
        List<ResourceTotal> totals,
        String totalCost,
        PeakInfo peak
) {
    public record RangeInfo(
            String from,
            String to,
            long days,
            String timezone,
            int bucketCount
    ) {
    }

    public record ResourceCatalogItem(
            String code,
            String name,
            String unit,
            String unitPrice
    ) {
    }

    public record UsagePoint(
            String start,
            String end,
            boolean partial,
            String quantity,
            String cost
    ) {
    }

    public record ResourceSeries(
            String resourceCode,
            List<UsagePoint> points
    ) {
    }

    public record ResourceTotal(
            String resourceCode,
            String quantity,
            String cost
    ) {
    }

    public record PeakInfo(
            String start,
            String end,
            String cost
    ) {
    }
}