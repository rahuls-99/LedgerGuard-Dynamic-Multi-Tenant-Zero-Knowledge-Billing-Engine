package com.ledgerguard.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

@Component
@Profile({"dev", "local"})
public class AnalyticsSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsSeeder.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AnalyticsSeeder(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String tenantId = "tenant_demo";
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = today;

        seedInvoiceIfAbsent(tenantId, "INV-DEMO-001", periodStart, periodEnd, "INR",
                new BigDecimal("4500.00"), new BigDecimal("810.00"), new BigDecimal("5310.00"), "ISSUED");

        seedUsageIfAbsent(tenantId, "API_CALLS", "API Calls", "req",
                new BigDecimal("15000.0000"), new BigDecimal("0.1000"), new BigDecimal("1500.00"), today.minusDays(2));
        seedUsageIfAbsent(tenantId, "STORAGE_GB", "Cloud Storage", "gb",
                new BigDecimal("120.0000"), new BigDecimal("25.0000"), new BigDecimal("3000.00"), today.minusDays(1));
        seedUsageIfAbsent(tenantId, "ZK_PROOFS", "ZK Proof Verifications", "proof",
                new BigDecimal("400.0000"), new BigDecimal("2.0250"), new BigDecimal("810.00"), today);

        log.info("AnalyticsSeeder completed idempotent demo data check for tenant={}", tenantId);
    }

    private void seedInvoiceIfAbsent(
            String tenantId,
            String invoiceNumber,
            LocalDate start,
            LocalDate end,
            String currency,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            String status
    ) {
        String checkSql = "SELECT COUNT(*) FROM billing_invoices WHERE invoice_number = :invoiceNumber";
        Integer count = jdbcTemplate.queryForObject(
                checkSql,
                new MapSqlParameterSource("invoiceNumber", invoiceNumber),
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }

        String insertSql = """
                INSERT INTO billing_invoices
                    (invoice_number, tenant_id, billing_period_start, billing_period_end, currency,
                     subtotal_amount, tax_amount, total_amount, status, issued_at)
                VALUES
                    (:invoiceNumber, :tenantId, :startDate, :endDate, :currency,
                     :subtotal, :tax, :total, :status, CURRENT_TIMESTAMP)
                """;
        jdbcTemplate.update(insertSql, new MapSqlParameterSource()
                .addValue("invoiceNumber", invoiceNumber)
                .addValue("tenantId", tenantId)
                .addValue("startDate", Date.valueOf(start))
                .addValue("endDate", Date.valueOf(end))
                .addValue("currency", currency)
                .addValue("subtotal", subtotal)
                .addValue("tax", tax)
                .addValue("total", total)
                .addValue("status", status));
    }

    private void seedUsageIfAbsent(
            String tenantId,
            String resourceCode,
            String resourceName,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            LocalDate recordedDate
    ) {
        String checkSql = """
                SELECT COUNT(*) FROM usage_records
                WHERE tenant_id = :tenantId
                  AND resource_code = :resourceCode
                  AND recorded_date = :recordedDate
                """;
        Integer count = jdbcTemplate.queryForObject(checkSql, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("resourceCode", resourceCode)
                .addValue("recordedDate", Date.valueOf(recordedDate)), Integer.class);

        if (count != null && count > 0) {
            return;
        }

        String insertSql = """
                INSERT INTO usage_records
                    (tenant_id, resource_code, resource_name, unit, quantity, unit_price, amount, recorded_date)
                VALUES
                    (:tenantId, :resourceCode, :resourceName, :unit, :quantity, :unitPrice, :amount, :recordedDate)
                """;
        jdbcTemplate.update(insertSql, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("resourceCode", resourceCode)
                .addValue("resourceName", resourceName)
                .addValue("unit", unit)
                .addValue("quantity", quantity)
                .addValue("unitPrice", unitPrice)
                .addValue("amount", amount)
                .addValue("recordedDate", Date.valueOf(recordedDate)));
    }
}