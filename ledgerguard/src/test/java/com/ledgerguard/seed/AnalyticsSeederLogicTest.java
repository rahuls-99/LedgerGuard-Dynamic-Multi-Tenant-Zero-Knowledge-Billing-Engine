package com.ledgerguard.seed;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class AnalyticsSeederLogicTest {
    @Test
    void profileGuardBlocksProdAndDefaultButAllowsDemo() {
        SeedProfileGuard prodGuard = new SeedProfileGuard(new MockEnvironment().withProperty("spring.profiles.active", "prod"));
        assertThrows(IllegalStateException.class, () -> prodGuard.assertSeedAllowed(true));
        SeedProfileGuard defaultGuard = new SeedProfileGuard(new MockEnvironment());
        assertThrows(IllegalStateException.class, () -> defaultGuard.assertSeedAllowed(true));
        SeedProfileGuard demoGuard = new SeedProfileGuard(new MockEnvironment().withProperty("spring.profiles.active", "demo"));
        demoGuard.assertSeedAllowed(true);
    }
    @Test
    void usageRollupMatchesRawRecordsAndRespectsOrgBComputeExclusion() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        UsagePatternGenerator gen = new UsagePatternGenerator();
        List<UsagePatternGenerator.RawUsageRow> run1 = gen.generate180Days(today, 0.25, true, new Random(123L));
        List<UsagePatternGenerator.RawUsageRow> run2 = gen.generate180Days(today, 0.25, true, new Random(123L));
        assertEquals(run1, run2);
        boolean hasCompute = run1.stream().anyMatch(r -> "COMPUTE_HOURS".equals(r.resourceCode()));
        assertTrue(!hasCompute, "orgB must not contain COMPUTE_HOURS");
        for (UsagePatternGenerator.RawUsageRow row : run1) {
            BigDecimal expected = row.quantity().multiply(row.unitPrice()).setScale(4, java.math.RoundingMode.HALF_UP);
            assertEquals(expected, row.amount());
        }
        List<UsagePatternGenerator.DailyRollupRow> daily = gen.rollupDaily(run1);
        BigDecimal rawSum = run1.stream()
                .map(UsagePatternGenerator.RawUsageRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, java.math.RoundingMode.HALF_UP);
        BigDecimal dailySum = daily.stream()
                .map(UsagePatternGenerator.DailyRollupRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, java.math.RoundingMode.HALF_UP);
        assertEquals(rawSum, dailySum);
    }
    @Test
    void transactionHistoryGeneratesChronologicalSeedIds() {
        LocalDate today = LocalDate.of(2026, 11, 20);
        TransactionHistoryGenerator txGen = new TransactionHistoryGenerator();
        List<String> wallets = List.of("W-A-ANALYTICS-1", "W-A-ANALYTICS-2");
        TransactionHistoryGenerator.HistoryResult res = txGen.generate90Days("A", today, 200, wallets, new Random(77L));
        assertEquals(200, res.transactions().size());
        assertTrue(res.transactions().get(0).transactionId().startsWith("TXN-SEED-A-"));
    }
}