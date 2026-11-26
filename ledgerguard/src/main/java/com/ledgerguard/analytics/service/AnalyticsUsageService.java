package com.ledgerguard.analytics.service;

import com.ledgerguard.analytics.dto.UsageResponse;
import com.ledgerguard.analytics.dto.UsageResponse.PeakInfo;
import com.ledgerguard.analytics.dto.UsageResponse.RangeInfo;
import com.ledgerguard.analytics.dto.UsageResponse.ResourceCatalogItem;
import com.ledgerguard.analytics.dto.UsageResponse.ResourceSeries;
import com.ledgerguard.analytics.dto.UsageResponse.ResourceTotal;
import com.ledgerguard.analytics.dto.UsageResponse.UsagePoint;
import com.ledgerguard.analytics.repo.UsageAnalyticsRepository;
import com.ledgerguard.analytics.repo.UsageAnalyticsRepository.ResourceMetaRow;
import com.ledgerguard.analytics.repo.UsageAnalyticsRepository.UsageDailyRow;
import com.ledgerguard.analytics.util.Buckets;
import com.ledgerguard.analytics.util.Buckets.Bucket;
import com.ledgerguard.analytics.util.Buckets.BucketSchedule;
import com.ledgerguard.analytics.util.Buckets.Granularity;
import com.ledgerguard.analytics.util.DecimalFormats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class AnalyticsUsageService {

    public record UnknownResourceItem(String field, String message) {
    }

    public static class UnknownResourcesException extends RuntimeException {
        private final List<UnknownResourceItem> errors;

        public UnknownResourcesException(List<UnknownResourceItem> errors) {
            super("Unknown resource code(s)");
            this.errors = List.copyOf(errors);
        }

        public List<UnknownResourceItem> getErrors() {
            return errors;
        }
    }

    private final UsageAnalyticsRepository repository;
    private final Clock utcClock;

    public AnalyticsUsageService(UsageAnalyticsRepository repository, Clock utcClock) {
        this.repository = repository;
        this.utcClock = utcClock;
    }

    @Transactional(readOnly = true, timeout = 3)
    public UsageResponse getUsage(LocalDate from,
                                  LocalDate to,
                                  String granularityRaw,
                                  String currencyRaw,
                                  List<String> requestedResources) {
        LocalDate todayUtc = LocalDate.ofInstant(java.time.Clock.systemUTC().instant(), java.time.ZoneOffset.UTC);
        Granularity granularity = Granularity.from(granularityRaw);
        String currency = (currencyRaw == null || currencyRaw.isBlank()) ? "USD" : currencyRaw.trim().toUpperCase();

        boolean resourceParamProvided = requestedResources != null;
        Set<String> dedupRequested = new LinkedHashSet<>();
        if (resourceParamProvided) {
            for (String code : requestedResources) {
                if (code != null) {
                    dedupRequested.add(code);
                }
            }
        }

        Map<String, ResourceMetaRow> catalogByCode = new HashMap<>();
        if (resourceParamProvided && !dedupRequested.isEmpty()) {
            List<ResourceMetaRow> found = repository.findResourcesByCodes(List.copyOf(dedupRequested));
            for (ResourceMetaRow row : found) {
                catalogByCode.put(row.code(), row);
            }
            List<UnknownResourceItem> unknownErrors = new ArrayList<>();
            for (String code : dedupRequested) {
                if (!catalogByCode.containsKey(code)) {
                    unknownErrors.add(new UnknownResourceItem("resource", "unknown resource: " + code));
                }
            }
            if (!unknownErrors.isEmpty()) {
                throw new UnknownResourcesException(unknownErrors);
            }
        }

        List<UsageDailyRow> rows = repository.findDailyRows(
                from,
                to,
                resourceParamProvided && !dedupRequested.isEmpty() ? List.copyOf(dedupRequested) : null
        );

        Set<String> activeCodes = new TreeSet<>();
        if (resourceParamProvided) {
            activeCodes.addAll(dedupRequested);
        } else {
            for (UsageDailyRow row : rows) {
                if (row.resourceCode() != null) {
                    activeCodes.add(row.resourceCode());
                    catalogByCode.putIfAbsent(
                            row.resourceCode(),
                            new ResourceMetaRow(row.resourceCode(), row.name(), row.unit(), row.unitPrice())
                    );
                }
            }
        }

        BucketSchedule schedule = Buckets.generate(from, to, granularity, todayUtc);
        List<Bucket> buckets = schedule.buckets();
        int bucketCount = buckets.size();

        Map<String, BigDecimal[]> qtyByResource = new HashMap<>();
        Map<String, BigDecimal[]> amtByResource = new HashMap<>();
        for (String code : activeCodes) {
            BigDecimal[] qArr = new BigDecimal[bucketCount];
            BigDecimal[] aArr = new BigDecimal[bucketCount];
            Arrays.fill(qArr, BigDecimal.ZERO);
            Arrays.fill(aArr, BigDecimal.ZERO);
            qtyByResource.put(code, qArr);
            amtByResource.put(code, aArr);
        }

        for (UsageDailyRow row : rows) {
            Integer idx = schedule.indexOf(row.usageDate());
            if (idx == null) {
                continue;
            }
            BigDecimal[] qArr = qtyByResource.get(row.resourceCode());
            BigDecimal[] aArr = amtByResource.get(row.resourceCode());
            if (qArr != null && aArr != null) {
                BigDecimal rowQty = row.quantity() == null ? BigDecimal.ZERO : row.quantity();
                BigDecimal rowAmt = row.amount() == null ? BigDecimal.ZERO : row.amount();
                qArr[idx] = qArr[idx].add(rowQty);
                aArr[idx] = aArr[idx].add(rowAmt);
            }
        }

        List<ResourceCatalogItem> resourceItems = new ArrayList<>(activeCodes.size());
        List<ResourceSeries> seriesList = new ArrayList<>(activeCodes.size());
        List<ResourceTotal> totalsList = new ArrayList<>(activeCodes.size());

        for (String code : activeCodes) {
            ResourceMetaRow meta = catalogByCode.get(code);
            String name = meta != null && meta.name() != null ? meta.name() : code;
            String unit = meta != null && meta.unit() != null ? meta.unit() : "";
            String unitPriceStr = meta != null && meta.unitPrice() != null
                    ? DecimalFormats.fmt4(meta.unitPrice())
                    : DecimalFormats.fmt4(BigDecimal.ZERO);

            resourceItems.add(new ResourceCatalogItem(code, name, unit, unitPriceStr));

            BigDecimal[] qArr = qtyByResource.get(code);
            BigDecimal[] aArr = amtByResource.get(code);

            BigDecimal resQtySum = BigDecimal.ZERO;
            BigDecimal resAmtSum = BigDecimal.ZERO;
            List<UsagePoint> points = new ArrayList<>(bucketCount);

            for (int i = 0; i < bucketCount; i++) {
                Bucket b = buckets.get(i);
                resQtySum = resQtySum.add(qArr[i]);
                resAmtSum = resAmtSum.add(aArr[i]);
                points.add(new UsagePoint(
                        b.start().toString(),
                        b.end().toString(),
                        b.partial(),
                        DecimalFormats.fmt4(qArr[i]),
                        DecimalFormats.fmt2(aArr[i])
                ));
            }

            seriesList.add(new ResourceSeries(code, points));
            totalsList.add(new ResourceTotal(
                    code,
                    DecimalFormats.fmt4(resQtySum),
                    DecimalFormats.fmt2(resAmtSum)
            ));
        }

        BigDecimal[] unroundedBucketTotals = new BigDecimal[bucketCount];
        Arrays.fill(unroundedBucketTotals, BigDecimal.ZERO);
        List<String> totalCostSeries = new ArrayList<>(bucketCount);

        BigDecimal grandTotalUnrounded = BigDecimal.ZERO;
        BigDecimal maxBucketAmount = BigDecimal.ZERO;
        int peakIndex = -1;

        for (int i = 0; i < bucketCount; i++) {
            BigDecimal bucketSum = BigDecimal.ZERO;
            for (String code : activeCodes) {
                bucketSum = bucketSum.add(amtByResource.get(code)[i]);
            }
            unroundedBucketTotals[i] = bucketSum;
            grandTotalUnrounded = grandTotalUnrounded.add(bucketSum);
            totalCostSeries.add(DecimalFormats.fmt2(bucketSum));

            if (bucketSum.compareTo(BigDecimal.ZERO) > 0 && bucketSum.compareTo(maxBucketAmount) > 0) {
                maxBucketAmount = bucketSum;
                peakIndex = i;
            }
        }

        PeakInfo peak = null;
        if (peakIndex >= 0) {
            Bucket peakBucket = buckets.get(peakIndex);
            peak = new PeakInfo(
                    peakBucket.start().toString(),
                    peakBucket.end().toString(),
                    DecimalFormats.fmt2(unroundedBucketTotals[peakIndex])
            );
        }

        long daysInclusive = ChronoUnit.DAYS.between(from, to) + 1;
        RangeInfo rangeInfo = new RangeInfo(
                from.toString(),
                to.toString(),
                daysInclusive,
                "UTC",
                bucketCount
        );

        seriesList.sort(Comparator.comparing(ResourceSeries::resourceCode));
        totalsList.sort(Comparator.comparing(ResourceTotal::resourceCode));

        return new UsageResponse(
                rangeInfo,
                granularity.name().toLowerCase(),
                currency,
                resourceItems,
                seriesList,
                totalCostSeries,
                totalsList,
                DecimalFormats.fmt2(grandTotalUnrounded),
                peak
        );
    }
}
