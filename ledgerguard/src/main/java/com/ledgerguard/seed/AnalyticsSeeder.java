package com.ledgerguard.seed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Profile({"demo", "e2e", "dev"})
public class AnalyticsSeeder implements ApplicationRunner {

    private final SeedProfileGuard profileGuard;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AnalyticsSeeder(
            SeedProfileGuard profileGuard,
            Clock clock,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.profileGuard = profileGuard;
        this.clock = clock;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean seedRequested = args.containsOption("seed-analytics");
        profileGuard.assertSeedAllowed(seedRequested);
        if (!seedRequested) {
            return;
        }

        List<String> tenants = parseTenants(args);
        String scale = getSingleOption(args, "scale", "normal");
        LocalDate today = args.containsOption("today")
                ? LocalDate.parse(getSingleOption(args, "today", LocalDate.now(clock).toString()))
                : LocalDate.now(clock);

        long startNs = System.nanoTime();
        UsagePatternGenerator usageGen = new UsagePatternGenerator();
        TransactionHistoryGenerator txGen = new TransactionHistoryGenerator();

        for (String tenant : tenants) {
            transactionTemplate.executeWithoutResult(status -> seedTenant(tenant, scale, today, usageGen, txGen));
        }

        double elapsedSec = (System.nanoTime() - startNs) / 1_000_000_000.0;
        log.info("Analytics seeding completed for tenants={} scale={} today={} in {}s",
                tenants, scale, today, String.format("%.2f", elapsedSec));
    }

    private void seedTenant(
            String tenant,
            String scale,
            LocalDate today,
            UsagePatternGenerator usageGen,
            TransactionHistoryGenerator txGen
    ) {
        // Idempotent cleanup of prior seeded usage & TXN-SEED-* rows (never touches E2E/non-seed data)
        jdbcTemplate.update("DELETE FROM ledger_entries WHERE transaction_id LIKE 'TXN-SEED-%'");
        jdbcTemplate.update("DELETE FROM transactions WHERE id LIKE 'TXN-SEED-%'");
        jdbcTemplate.update("DELETE FROM usage_daily");
        jdbcTemplate.update("DELETE FROM usage_records");

        if ("orgC".equalsIgnoreCase(tenant)) {
            log.info("Tenant {} intentionally seeded with empty analytics data", tenant);
            return;
        }

        boolean isOrgB = "orgB".equalsIgnoreCase(tenant);
        double multiplier = isOrgB ? 0.25 : ("large".equalsIgnoreCase(scale) ? 12.0 : 1.0);
        int txCount = isOrgB ? 400 : 1500;
        String prefix = isOrgB ? "B" : "A";
        List<String> wallets = List.of(
                "W-" + prefix + "-ANALYTICS-1",
                "W-" + prefix + "-ANALYTICS-2",
                "W-" + prefix + "-ANALYTICS-3",
                "W-" + prefix + "-ANALYTICS-4",
                "W-" + prefix + "-ANALYTICS-5"
        );

        Random random = new Random(tenant.hashCode() ^ 31L);
        List<UsagePatternGenerator.RawUsageRow> rawRows =
                usageGen.generate180Days(today, multiplier, isOrgB, random);
        List<UsagePatternGenerator.DailyRollupRow> dailyRows = usageGen.rollupDaily(rawRows);

        jdbcTemplate.batchUpdate(
                "INSERT INTO usage_records (resource_code, quantity, unit_price, amount, occurred_at, source) VALUES (?, ?, ?, ?, ?, ?)",
                rawRows,
                1000,
                (ps, row) -> {
                    ps.setString(1, row.resourceCode());
                    ps.setBigDecimal(2, row.quantity());
                    ps.setBigDecimal(3, row.unitPrice());
                    ps.setBigDecimal(4, row.amount());
                    ps.setTimestamp(5, Timestamp.from(row.occurredAt()));
                    ps.setString(6, row.source());
                }
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO usage_daily (usage_date, resource_code, quantity, amount, record_count) VALUES (?, ?, ?, ?, ?)",
                dailyRows,
                500,
                (ps, row) -> {
                    ps.setDate(1, java.sql.Date.valueOf(row.usageDate()));
                    ps.setString(2, row.resourceCode());
                    ps.setBigDecimal(3, row.quantity());
                    ps.setBigDecimal(4, row.amount());
                    ps.setInt(5, row.recordCount());
                }
        );

        TransactionHistoryGenerator.HistoryResult txResult =
                txGen.generate90Days(prefix, today, txCount, wallets, random);

        jdbcTemplate.batchUpdate(
                "INSERT INTO transactions (id, wallet_id, type, status, amount, failure_reason, created_at, balance_after) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                txResult.transactions(),
                500,
                (ps, tx) -> {
                    ps.setString(1, tx.transactionId());
                    ps.setString(2, tx.walletId());
                    ps.setString(3, tx.type());
                    ps.setString(4, tx.status());
                    ps.setBigDecimal(5, tx.amount());
                    ps.setString(6, tx.failureReason());
                    ps.setTimestamp(7, Timestamp.from(tx.createdAt()));
                    ps.setBigDecimal(8, tx.balanceAfter());
                }
        );
    }

    private List<String> parseTenants(ApplicationArguments args) {
        List<String> values = args.getOptionValues("tenants");
        if (values == null || values.isEmpty()) {
            return List.of("orgA", "orgB");
        }
        return Arrays.stream(values.get(0).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String getSingleOption(ApplicationArguments args, String name, String fallback) {
        List<String> values = args.getOptionValues(name);
        return (values == null || values.isEmpty()) ? fallback : values.get(0);
    }
}