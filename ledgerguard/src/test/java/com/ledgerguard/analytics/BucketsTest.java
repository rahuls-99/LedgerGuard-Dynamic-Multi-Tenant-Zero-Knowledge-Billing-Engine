package com.ledgerguard.analytics;

import com.ledgerguard.analytics.util.Buckets;
import com.ledgerguard.analytics.util.Buckets.Bucket;
import com.ledgerguard.analytics.util.Buckets.BucketSchedule;
import com.ledgerguard.analytics.util.Buckets.Granularity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BucketsTest {

    @Test
    void weekBuckets_2026_10_22_to_2026_11_20_fiveBuckets() {
        BucketSchedule schedule = Buckets.generate(
                LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 11, 20),
                Granularity.WEEK,
                LocalDate.of(2026, 11, 20)
        );
        List<Bucket> b = schedule.buckets();
        assertEquals(5, b.size());
        assertEquals(LocalDate.of(2026, 10, 22), b.get(0).start());
        assertEquals(LocalDate.of(2026, 10, 25), b.get(0).end());
        assertTrue(b.get(0).partial());

        assertEquals(LocalDate.of(2026, 11, 16), b.get(4).start());
        assertEquals(LocalDate.of(2026, 11, 20), b.get(4).end());
        assertTrue(b.get(4).partial());
    }

    @Test
    void monthBuckets_partialFlag() {
        BucketSchedule schedule = Buckets.generate(
                LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 11, 20),
                Granularity.MONTH,
                LocalDate.of(2026, 11, 20)
        );
        assertEquals(2, schedule.buckets().size());
        assertTrue(schedule.buckets().get(0).partial());
        assertTrue(schedule.buckets().get(1).partial());
    }

    @Test
    void yearBoundaryAndLeapYearFebruary() {
        BucketSchedule yearBoundary = Buckets.generate(
                LocalDate.of(2026, 12, 20),
                LocalDate.of(2027, 1, 10),
                Granularity.WEEK,
                LocalDate.of(2027, 2, 1)
        );
        assertEquals(4, yearBoundary.buckets().size());
        assertTrue(yearBoundary.buckets().get(0).partial());
        assertEquals(LocalDate.of(2026, 12, 28), yearBoundary.buckets().get(2).start());
        assertEquals(LocalDate.of(2027, 1, 3), yearBoundary.buckets().get(2).end());

        BucketSchedule leapFeb = Buckets.generate(
                LocalDate.of(2028, 2, 1),
                LocalDate.of(2028, 3, 5),
                Granularity.MONTH,
                LocalDate.of(2028, 3, 15)
        );
        assertEquals(LocalDate.of(2028, 2, 1), leapFeb.buckets().get(0).start());
        assertEquals(LocalDate.of(2028, 2, 29), leapFeb.buckets().get(0).end());
        assertFalse(leapFeb.buckets().get(0).partial());
        assertTrue(leapFeb.buckets().get(1).partial());
    }

    @Test
    void staticCheck_noForbiddenTimeCallsInAnalyticsPackage() throws IOException {
        Path analyticsDir = Path.of("src/main/java/com/ledgerguard/analytics");
        if (!Files.exists(analyticsDir)) {
            return;
        }
        List<String> forbidden = List.of(
                "LocalDate.now(",
                "LocalDateTime.now(",
                "Instant.now(",
                "ZoneId.systemDefault(",
                "WeekFields",
                "java.util.Date"
        );
        try (Stream<Path> walk = Files.walk(analyticsDir)) {
            List<Path> javaFiles = walk.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                for (String token : forbidden) {
                    assertFalse(content.contains(token),
                            "Forbidden token '" + token + "' found in " + file);
                }
            }
        }
    }
}