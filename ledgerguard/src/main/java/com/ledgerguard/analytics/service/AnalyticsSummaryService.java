package com.ledgerguard.analytics.service;

import com.ledgerguard.dto.SummaryResponse;
import com.ledgerguard.dto.SummaryResponse.ChangeSummary;
import com.ledgerguard.dto.SummaryResponse.PeriodSummary;
import com.ledgerguard.dto.SummaryResponse.ResourceUsageItem;
import com.ledgerguard.dto.SummaryResponse.UsageSummary;
import com.ledgerguard.analytics.repo.TransactionAnalyticsRepository;
import com.ledgerguard.analytics.repo.TransactionAnalyticsRepository.TransactionBucketRow;
import com.ledgerguard.analytics.repo.UsageAnalyticsRepository;
import com.ledgerguard.analytics.repo.UsageAnalyticsRepository.UsageAggregateRow;
import com.ledgerguard.analytics.util.DateRanges;
import com.ledgerguard.analytics.util.DateRanges.RangeWindow;
import com.ledgerguard.analytics.util.PercentMath;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AnalyticsSummaryService {

    private static final int QUERY_TIMEOUT_SECONDS = 3;

    private final TransactionAnalyticsRepository txRepo;
    private final UsageAnalyticsRepository usageRepo;

    public AnalyticsSummaryService(
            TransactionAnalyticsRepository txRepo,
            UsageAnalyticsRepository usageRepo
    ) {
        this.txRepo = txRepo;
        this.usageRepo = usageRepo;
    }

    private static final class PeriodAcc {
        long success;
        long failed;
        long rolledBack;
        long otherCurrencyCount;
        BigDecimal charged = BigDecimal.ZERO;
        BigDecimal toppedUp = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;

        long total() {
            return success + failed + rolledBack;
        }

        PeriodSummary toDto(String baseCurrency) {
            long total = total();
            return new PeriodSummary(
                    total,
                    success,
                    failed,
                    rolledBack,
                    otherCurrencyCount,
                    PercentMath.successRate(success, total),
                    charged.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    toppedUp.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    refunded.setScale(2, RoundingMode.HALF_UP).toPlainString()
            );
        }
    }

    @Transactional(readOnly = true, timeout = QUERY_TIMEOUT_SECONDS)
    public SummaryResponse getSummary(String tenantId, String baseCurrency, LocalDate from, LocalDate to) {
        RangeWindow window = DateRanges.fromUtcDates(from, to);
        String normalizedBase = (baseCurrency == null || baseCurrency.isBlank())
                ? "INR"
                : baseCurrency.trim().toUpperCase(Locale.ROOT);

        List<TransactionBucketRow> txRows = txRepo.aggregateBuckets(
                window.prevFromUtc(),
                window.curFromUtc(),
                window.toExclusiveUtc(),
                QUERY_TIMEOUT_SECONDS
        );

        List<UsageAggregateRow> usageRows = usageRepo.aggregateUsage(
                window.prevFromInclusive(),
                window.fromInclusive(),
                window.toInclusive(),
                QUERY_TIMEOUT_SECONDS
        );

        PeriodAcc curAcc = new PeriodAcc();
        PeriodAcc prevAcc = new PeriodAcc();

        for (TransactionBucketRow row : txRows) {
            PeriodAcc target = row.currentPeriod() ? curAcc : prevAcc;
            String status = row.status() == null ? "" : row.status().toUpperCase(Locale.ROOT);
            String type = row.type() == null ? "" : row.type().toUpperCase(Locale.ROOT);
            String ccy = row.currency() == null ? "" : row.currency().trim().toUpperCase(Locale.ROOT);

            switch (status) {
                case "SUCCESS" -> target.success += row.count();
                case "FAILED" -> target.failed += row.count();
                case "ROLLED_BACK" -> target.rolledBack += row.count();
                default -> {
                    continue;
                }
            }

            if (!normalizedBase.equals(ccy)) {
                target.otherCurrencyCount += row.count();
            } else if ("SUCCESS".equals(status)) {
                switch (type) {
                    case "CHARGE" -> target.charged = target.charged.add(row.totalAmount());
                    case "TOPUP" -> target.toppedUp = target.toppedUp.add(row.totalAmount());
                    case "REFUND" -> target.refunded = target.refunded.add(row.totalAmount());
                    default -> { }
                }
            }
        }

        BigDecimal unroundedTotalCost = BigDecimal.ZERO;
        BigDecimal unroundedPrevCost = BigDecimal.ZERO;
        List<ResourceUsageItem> activeResources = usageRows.stream()
                .filter(r -> r.currentRows() > 0)
                .sorted(Comparator
                        .comparing(UsageAggregateRow::currentAmount, Comparator.reverseOrder())
                        .thenComparing(UsageAggregateRow::code))
                .map(r -> {
                    String currentAmtStr = r.currentAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
                    return new ResourceUsageItem(
                            r.code(),
                            r.name(),
                            r.unit(),
                            r.currentQuantity().longValue(),
                            currentAmtStr,
                            "0.00"
                    );
                })
                .toList();
        for (UsageAggregateRow r : usageRows) {
            if (r.currentRows() > 0) {
                unroundedTotalCost = unroundedTotalCost.add(r.currentAmount());
            }
            unroundedPrevCost = unroundedPrevCost.add(r.previousAmount());
        }
        String totalCostStr = unroundedTotalCost.setScale(2, RoundingMode.HALF_UP).toPlainString();
        BigDecimal curSuccessRate = new BigDecimal(PercentMath.successRate(curAcc.success, curAcc.total()));
        BigDecimal prevSuccessRate = new BigDecimal(PercentMath.successRate(prevAcc.success, prevAcc.total()));
        ChangeSummary changes = new ChangeSummary(
                PercentMath.changePercent(curAcc.total(), prevAcc.total()),
                PercentMath.changePercent(curAcc.charged, prevAcc.charged),
                PercentMath.changePercent(curSuccessRate, prevSuccessRate)
        );
        UsageSummary usageSummary = new UsageSummary(
                totalCostStr,
                activeResources
        );
        return new SummaryResponse(
                tenantId,
                normalizedBase,
                "day",
                new SummaryResponse.WindowRange(
                        window.fromInclusive().toString(),
                        window.toInclusive().toString()
                ),
                new SummaryResponse.WindowRange(
                        window.prevFromInclusive().toString(),
                        window.prevToInclusive().toString()
                ),
                curAcc.toDto(normalizedBase),
                prevAcc.toDto(normalizedBase),
                changes,
                usageSummary
        );
    }
}
