package com.ledgerguard.analytics.repo;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
@Repository
public class TransactionAnalyticsRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    public TransactionAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    public record TransactionBucketRow(
            boolean currentPeriod,
            String status,
            String type,
            String currency,
            long count,
            BigDecimal totalAmount
    ) {}
    public record DailyOutcomeRow(
            LocalDate day,
            String status,
            String type,
            String currency,
            long count,
            BigDecimal sumAmount
    ) {}
    public List<TransactionBucketRow> aggregateBuckets(
            LocalDateTime prevFromUtc,
            LocalDateTime curFromUtc,
            LocalDateTime toExclusiveUtc,
            int queryTimeoutSeconds) {
        String sql = """
                SELECT (created_at >= :curFromUtc) AS is_current,
                       status,
                       type,
                       currency,
                       COUNT(*) AS cnt,
                       COALESCE(SUM(amount), 0) AS total_amt
                FROM billing_transactions
                WHERE created_at >= :prevFromUtc
                  AND created_at < :toExclusiveUtc
                GROUP BY is_current, status, type, currency
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("prevFromUtc", prevFromUtc)
                .addValue("curFromUtc", curFromUtc)
                .addValue("toExclusiveUtc", toExclusiveUtc);
        jdbcTemplate.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            boolean current = rs.getBoolean("is_current");
            BigDecimal total = rs.getBigDecimal("total_amt");
            return new TransactionBucketRow(
                    current,
                    rs.getString("status"),
                    rs.getString("type"),
                    rs.getString("currency"),
                    rs.getLong("cnt"),
                    total != null ? total : BigDecimal.ZERO
            );
        });
    }
    public List<DailyOutcomeRow> findDailyOutcomes(
            LocalDateTime fromStart,
            LocalDateTime toExclusive,
            List<String> types,
            int queryTimeoutSeconds) {
        boolean filterTypes = types != null && !types.isEmpty();
        StringBuilder sql = new StringBuilder("""
                SELECT DATE(created_at) AS d,
                       status,
                       type,
                       currency,
                       COUNT(*) AS cnt,
                       SUM(amount) AS amt
                FROM billing_transactions
                WHERE created_at >= :fromStart
                  AND created_at < :toExclusive
                  AND status IN ('SUCCESS', 'FAILED', 'ROLLED_BACK')
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromStart", fromStart)
                .addValue("toExclusive", toExclusive);
        if (filterTypes) {
            sql.append("  AND type IN (:types)\n");
            params.addValue("types", types);
        }
        sql.append("GROUP BY d, status, type, currency\nORDER BY d ASC");
        jdbcTemplate.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            Date sqlDate = rs.getDate("d");
            LocalDate day = sqlDate != null ? sqlDate.toLocalDate() : LocalDate.ofInstant(
                    ((Timestamp) rs.getObject("d")).toInstant(), ZoneOffset.UTC);
            BigDecimal sum = rs.getBigDecimal("amt");
            return new DailyOutcomeRow(
                    day,
                    rs.getString("status"),
                    rs.getString("type"),
                    rs.getString("currency"),
                    rs.getLong("cnt"),
                    sum != null ? sum : BigDecimal.ZERO
            );
        });
    }
}