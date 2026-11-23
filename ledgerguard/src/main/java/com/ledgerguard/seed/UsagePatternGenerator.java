package com.ledgerguard.seed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class UsagePatternGenerator {

    public record RawUsageRow(
            String resourceCode,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            Instant occurredAt,
            String source
    ) {}

    public record DailyRollupRow(
            LocalDate usageDate,
            String resourceCode,
            BigDecimal quantity,
            BigDecimal amount,
            int recordCount
    ) {}

    public record ResourceSpec(String code, BigDecimal baseDailyQty, BigDecimal unitPrice) {}

    public static final List<ResourceSpec> DEFAULT_RESOURCES = List.of(
            new ResourceSpec("API_CALLS", new BigDecimal("2000.0000"), new BigDecimal("0.0004")),
            new ResourceSpec("STORAGE_GB", new BigDecimal("100.0000"), new BigDecimal("0.0500")),
            new ResourceSpec("COMPUTE_HOURS", new BigDecimal("40.0000"), new BigDecimal("1.5000")),
            new ResourceSpec("SEATS", new BigDecimal("25.0000"), new BigDecimal("0.5000"))
    );

    public List<RawUsageRow> generate180Days(
            LocalDate today,
            double volumeMultiplier,
            boolean excludeComputeHours,
            Random random
    ) {
        LocalDate startDate = today.minusDays(179);
        LocalDate spikeDate = today.minusDays(20);
        LocalDate outageDate = today.minusDays(40);
        List<RawUsageRow> rows = new ArrayList<>();

        for (int dayOffset = 0; dayOffset < 180; dayOffset++) {
            LocalDate currentDate = startDate.plusDays(dayOffset);
            if (currentDate.equals(outageDate)) {
                continue;
            }

            boolean isWeekend = currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                    || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY;
            double weekendFactor = isWeekend ? 0.40 : 1.0;
            long weeksElapsed = Math.max(0, dayOffset / 7L);
            double growthFactor = Math.pow(1.015, weeksElapsed);
            int monthlyStep = (currentDate.getDayOfMonth() == 1) ? 1 : 0;

            for (ResourceSpec spec : DEFAULT_RESOURCES) {
                if (excludeComputeHours && "COMPUTE_HOURS".equals(spec.code())) {
                    continue;
                }
                if ("COMPUTE_HOURS".equals(spec.code()) && isWeekend) {
                    continue;
                }

                int recordsPerDay = 20;
                double baseForDay = spec.baseDailyQty().doubleValue()
                        * volumeMultiplier
                        * weekendFactor
                        * growthFactor;

                if ("STORAGE_GB".equals(spec.code())) {
                    baseForDay += monthlyStep * 15.0 * volumeMultiplier;
                }

                if ("API_CALLS".equals(spec.code()) && currentDate.equals(spikeDate)) {
                    baseForDay *= 3.0;
                }

                double perRecordTarget = Math.max(0.0001, baseForDay / recordsPerDay);

                for (int i = 0; i < recordsPerDay; i++) {
                    double jitter = 0.85 + (random.nextDouble() * 0.30);
                    BigDecimal qty = BigDecimal.valueOf(perRecordTarget * jitter)
                            .setScale(4, RoundingMode.HALF_UP);
                    if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                        qty = new BigDecimal("0.0001");
                    }
                    BigDecimal amount = qty.multiply(spec.unitPrice())
                            .setScale(4, RoundingMode.HALF_UP);
                    Instant occurredAt = currentDate
                            .atStartOfDay(ZoneOffset.UTC)
                            .plusSeconds((long) (i * 3600 + random.nextInt(1800)))
                            .toInstant();

                    rows.add(new RawUsageRow(
                            spec.code(),
                            qty,
                            spec.unitPrice(),
                            amount,
                            occurredAt,
                            "SYSTEM"
                    ));
                }
            }
        }
        return rows;
    }

    public List<DailyRollupRow> rollupDaily(List<RawUsageRow> rawRows) {
        record Key(LocalDate date, String resourceCode) {}
        record Acc(BigDecimal qty, BigDecimal amt, int count) {}

        Map<Key, Acc> map = new LinkedHashMap<>();
        for (RawUsageRow row : rawRows) {
            LocalDate date = row.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();
            Key key = new Key(date, row.resourceCode());
            Acc current = map.getOrDefault(key, new Acc(BigDecimal.ZERO, BigDecimal.ZERO, 0));
            map.put(key, new Acc(
                    current.qty().add(row.quantity()),
                    current.amt().add(row.amount()),
                    current.count() + 1
            ));
        }

        List<DailyRollupRow> result = new ArrayList<>(map.size());
        for (Map.Entry<Key, Acc> entry : map.entrySet()) {
            result.add(new DailyRollupRow(
                    entry.getKey().date(),
                    entry.getKey().resourceCode(),
                    entry.getValue().qty().setScale(4, RoundingMode.HALF_UP),
                    entry.getValue().amt().setScale(4, RoundingMode.HALF_UP),
                    entry.getValue().count()
            ));
        }
        return result;
    }
}