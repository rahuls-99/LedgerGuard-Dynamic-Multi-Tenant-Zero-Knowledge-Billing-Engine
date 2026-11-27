package com.ledgerguard.analytics.service;
import com.ledgerguard.analytics.dto.TrendResponse;
import com.ledgerguard.analytics.dto.TrendResponse.Filters;
import com.ledgerguard.analytics.dto.TrendResponse.PeakBucket;
import com.ledgerguard.analytics.dto.TrendResponse.TrendBucket;
import com.ledgerguard.analytics.dto.TrendResponse.TrendTotals;
import com.ledgerguard.analytics.repo.TransactionAnalyticsRepository;
import com.ledgerguard.analytics.repo.TransactionAnalyticsRepository.DailyOutcomeRow;
import com.ledgerguard.analytics.util.PercentMath;
import com.ledgerguard.multitenancy.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
@Service
public class AnalyticsTrendService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsTrendService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("CHARGE", "TOPUP", "REFUND");
    private final TransactionAnalyticsRepository repository;
    private final Clock utcClock;
    private final Timer trendTimer;
    private final int timeoutSeconds;
    private final long slowWarnMs;
    public AnalyticsTrendService(
            TransactionAnalyticsRepository repository,
            Clock utcClock,
            MeterRegistry meterRegistry,
            @Value("${ledgerguard.analytics.trend-timeout-seconds:3}") int timeoutSeconds,
            @Value("${ledgerguard.analytics.slow-query-warn-ms:100}") long slowWarnMs) {
        this.repository = repository;
        this.utcClock = utcClock;
        this.trendTimer = Timer.builder("analytics.trend.query")
                .description("Duration of /api/analytics/transactions/trend query and aggregation")
                .register(meterRegistry);
        this.timeoutSeconds = timeoutSeconds;
        this.slowWarnMs = slowWarnMs;
    }
    private record Window(LocalDate start, LocalDate end, boolean partial) {}
    public static String successRateOrNull(long success, long total) {
        if (total <= 0) {
            return null;
        }
        return PercentMath.successRate(success, total);
    }
    public List<String> normalizeAndValidateTypes(List<String> rawTypes) {
        if (rawTypes == null || rawTypes.isEmpty()) {
            return List.of();
        }
        Set<String> sorted = new TreeSet<>();
        Set<String> invalid = new LinkedHashSet<>();
        for (String raw : rawTypes) {
            if (raw == null || raw.isBlank()) {
                invalid.add(String.valueOf(raw));
                continue;
            }
            String upper = raw.trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_TYPES.contains(upper)) {
                invalid.add(raw.trim());
            } else {
                sorted.add(upper);
            }
        }
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Invalid transaction type(s): " + String.join(", ", invalid));
        }
        return List.copyOf(sorted);
    }
    private List<Window> buildWindows(LocalDate from, LocalDate to, String granularity, LocalDate todayUtc) {
        String g = (granularity == null || granularity.isBlank())
                ? "day"
                : granularity.trim().toLowerCase(Locale.ROOT);
        List<Window> out = new ArrayList<>();
        switch (g) {
            case "week" -> {
                LocalDate cursorStart = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                while (!cursorStart.isAfter(to)) {
                    LocalDate naturalEnd = cursorStart.plusDays(6);
                    LocalDate start = cursorStart.isBefore(from) ? from : cursorStart;
                    LocalDate end = naturalEnd.isAfter(to) ? to : naturalEnd;
                    boolean partial = start.isAfter(cursorStart)
                            || end.isBefore(naturalEnd)
                            || (!end.isBefore(todayUtc) && !todayUtc.isAfter(end));
                    out.add(new Window(start, end, partial));
                    cursorStart = cursorStart.plusWeeks(1);
                }
            }
            case "month" -> {
                YearMonth cursorYm = YearMonth.from(from);
                YearMonth endYm = YearMonth.from(to);
                while (!cursorYm.isAfter(endYm)) {
                    LocalDate naturalStart = cursorYm.atDay(1);
                    LocalDate naturalEnd = cursorYm.atEndOfMonth();
                    LocalDate start = naturalStart.isBefore(from) ? from : naturalStart;
                    LocalDate end = naturalEnd.isAfter(to) ? to : naturalEnd;
                    boolean partial = start.isAfter(naturalStart)
                            || end.isBefore(naturalEnd)
                            || (!end.isBefore(todayUtc) && !todayUtc.isAfter(end));
                    out.add(new Window(start, end, partial));
                    cursorYm = cursorYm.plusMonths(1);
                }
            }
            default -> {
                LocalDate d = from;
                while (!d.isAfter(to)) {
                    boolean partial = d.equals(todayUtc);
                    out.add(new Window(d, d, partial));
                    d = d.plusDays(1);
                }
            }
        }
        return out;
    }
    private int findBucketIndex(List<Window> windows, LocalDate day) {
        if (day == null) {
            return -1;
        }
        for (int i = 0; i < windows.size(); i++) {
            Window w = windows.get(i);
            if (!day.isBefore(w.start()) && !day.isAfter(w.end())) {
                return i;
            }
        }
        return -1;
    }
    @Transactional(readOnly = true)
    public TrendResponse getTrend(
            LocalDate from,
            LocalDate to,
            String granularity,
            List<String> rawTypes,
            String baseCurrency) {
        String tenantId = TenantContext.getTenantId();
        long startNanos = System.nanoTime();
        try {
            return executeTrendQuery(from, to, granularity, rawTypes, baseCurrency);
        } finally {
            long elapsedNanos = System.nanoTime() - startNanos;
            trendTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
            if (elapsedMs > slowWarnMs) {
                log.warn("Slow trend query tenant={} granularity={} elapsedMs={}",
                        tenantId, granularity, elapsedMs);
            }
        }
    }
    private TrendResponse executeTrendQuery(
            LocalDate from,
            LocalDate to,
            String granularity,
            List<String> rawTypes,
            String baseCurrency) {
        LocalDate todayUtc = LocalDate.ofInstant(utcClock.instant(), ZoneOffset.UTC);
        String normalizedGran = (granularity == null || granularity.isBlank())
                ? "day"
                : granularity.trim().toLowerCase(Locale.ROOT);
        String currency = (baseCurrency == null || baseCurrency.isBlank())
                ? "USD"
                : baseCurrency.trim().toUpperCase(Locale.ROOT);
        List<String> normalizedTypes = normalizeAndValidateTypes(rawTypes);
        List<Window> windows = buildWindows(from, to, normalizedGran, todayUtc);
        LocalDateTime fromStart = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();
        List<DailyOutcomeRow> rows = repository.findDailyOutcomes(
                fromStart, toExclusive, normalizedTypes, timeoutSeconds);
        int n = windows.size();
        long[] successArr = new long[n];
        long[] failedArr = new long[n];
        long[] rolledBackArr = new long[n];
        BigDecimal[] chargeSumArr = new BigDecimal[n];
        for (int i = 0; i < n; i++) {
            chargeSumArr[i] = BigDecimal.ZERO;
        }
        long totalSuccess = 0;
        long totalFailed = 0;
        long totalRolledBack = 0;
        BigDecimal totalChargeAmount = BigDecimal.ZERO;
        long otherCurrencyCount = 0;
        for (DailyOutcomeRow row : rows) {
            if ("PENDING".equalsIgnoreCase(row.status())) {
                continue;
            }
            if (!currency.equalsIgnoreCase(row.currency())) {
                otherCurrencyCount += row.count();
            }
            int idx = findBucketIndex(windows, row.day());
            if (idx < 0) {
                continue;
            }
            switch (row.status()) {
                case "SUCCESS" -> {
                    successArr[idx] += row.count();
                    totalSuccess += row.count();
                    if ("CHARGE".equalsIgnoreCase(row.type()) && currency.equalsIgnoreCase(row.currency())) {
                        chargeSumArr[idx] = chargeSumArr[idx].add(row.sumAmount());
                        totalChargeAmount = totalChargeAmount.add(row.sumAmount());
                    }
                }
                case "FAILED" -> {
                    failedArr[idx] += row.count();
                    totalFailed += row.count();
                }
                case "ROLLED_BACK" -> {
                    rolledBackArr[idx] += row.count();
                    totalRolledBack += row.count();
                }
                default -> {
                }
            }
        }
        List<TrendBucket> resultBuckets = new ArrayList<>(n);
        long peakTotal = 0;
        LocalDate peakStart = null;
        for (int i = 0; i < n; i++) {
            Window w = windows.get(i);
            long bTotal = successArr[i] + failedArr[i] + rolledBackArr[i];
            if (bTotal > peakTotal) {
                peakTotal = bTotal;
                peakStart = w.start();
            }
            String chargedStr = chargeSumArr[i].setScale(2, RoundingMode.HALF_UP).toPlainString();
            resultBuckets.add(new TrendBucket(
                    w.start(),
                    w.end(),
                    w.partial(),
                    bTotal,
                    successArr[i],
                    failedArr[i],
                    rolledBackArr[i],
                    successRateOrNull(successArr[i], bTotal),
                    chargedStr
            ));
        }
        long grandTotal = totalSuccess + totalFailed + totalRolledBack;
        TrendTotals totals = new TrendTotals(
                grandTotal,
                totalSuccess,
                totalFailed,
                totalRolledBack,
                successRateOrNull(totalSuccess, grandTotal),
                totalChargeAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );
        PeakBucket peak = (peakStart != null && peakTotal > 0)
                ? new PeakBucket(peakStart, peakTotal)
                : null;
        return new TrendResponse(
                normalizedGran,
                currency,
                from,
                to,
                new Filters(normalizedTypes, otherCurrencyCount),
                resultBuckets,
                totals,
                peak
        );
    }
}