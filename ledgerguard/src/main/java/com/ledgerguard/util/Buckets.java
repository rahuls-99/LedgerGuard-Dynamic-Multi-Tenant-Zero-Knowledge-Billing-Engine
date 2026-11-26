package com.ledgerguard.analytics.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class Buckets {

    public enum Granularity {
        DAY,
        WEEK,
        MONTH;

        public static Granularity from(String raw) {
            if (raw == null) {
                throw new IllegalArgumentException("granularity is required");
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "day" -> DAY;
                case "week" -> WEEK;
                case "month" -> MONTH;
                default -> throw new IllegalArgumentException("unsupported granularity: " + raw);
            };
        }
    }

    public record Bucket(LocalDate start, LocalDate end, boolean partial) {
    }

    public record BucketSchedule(List<Bucket> buckets, Map<LocalDate, Integer> dateToIndex) {
        public Integer indexOf(LocalDate date) {
            return dateToIndex.get(date);
        }
    }

    private Buckets() {
    }

    public static BucketSchedule generate(LocalDate from,
                                          LocalDate to,
                                          Granularity granularity,
                                          LocalDate todayUtc) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(granularity, "granularity must not be null");
        Objects.requireNonNull(todayUtc, "todayUtc must not be null");

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }

        List<Bucket> buckets = new ArrayList<>();

        switch (granularity) {
            case DAY -> {
                LocalDate cursor = from;
                while (!cursor.isAfter(to)) {
                    boolean partial = !cursor.isBefore(todayUtc);
                    buckets.add(new Bucket(cursor, cursor, partial));
                    cursor = cursor.plusDays(1);
                }
            }
            case WEEK -> {
                LocalDate currentWeekStart = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                while (!currentWeekStart.isAfter(to)) {
                    LocalDate periodEnd = currentWeekStart.plusDays(6);
                    LocalDate clippedStart = currentWeekStart.isBefore(from) ? from : currentWeekStart;
                    LocalDate clippedEnd = periodEnd.isAfter(to) ? to : periodEnd;
                    boolean clipped = !clippedStart.equals(currentWeekStart) || !clippedEnd.equals(periodEnd);
                    boolean notEnded = !periodEnd.isBefore(todayUtc);
                    buckets.add(new Bucket(clippedStart, clippedEnd, clipped || notEnded));
                    currentWeekStart = currentWeekStart.plusWeeks(1);
                }
            }
            case MONTH -> {
                YearMonth currentMonth = YearMonth.from(from);
                YearMonth endMonth = YearMonth.from(to);
                while (!currentMonth.isAfter(endMonth)) {
                    LocalDate monthStart = currentMonth.atDay(1);
                    LocalDate monthEnd = currentMonth.atEndOfMonth();
                    LocalDate clippedStart = monthStart.isBefore(from) ? from : monthStart;
                    LocalDate clippedEnd = monthEnd.isAfter(to) ? to : monthEnd;
                    boolean clipped = !clippedStart.equals(monthStart) || !clippedEnd.equals(monthEnd);
                    boolean notEnded = !monthEnd.isBefore(todayUtc);
                    buckets.add(new Bucket(clippedStart, clippedEnd, clipped || notEnded));
                    currentMonth = currentMonth.plusMonths(1);
                }
            }
        }

        Map<LocalDate, Integer> indexMap = new HashMap<>(Math.max(16, buckets.size() * 2));
        for (int i = 0; i < buckets.size(); i++) {
            Bucket b = buckets.get(i);
            LocalDate d = b.start();
            while (!d.isAfter(b.end())) {
                indexMap.put(d, i);
                d = d.plusDays(1);
            }
        }

        return new BucketSchedule(List.copyOf(buckets), Map.copyOf(indexMap));
    }
}