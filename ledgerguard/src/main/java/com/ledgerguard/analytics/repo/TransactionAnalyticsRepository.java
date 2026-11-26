package com.ledgerguard.analytics.repo;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TransactionAnalyticsRepository {

    public record TransactionBucketRow(
            boolean currentPeriod,
            String status,
            String type,
            String currency,
            long count,
            BigDecimal totalAmount
    ) {}

    private static final String SQL = """
            SELECT (created_at >= :curFrom) AS is_current,
                   status,
                   type,
                   currency,
                   COUNT(*) AS cnt,
                   SUM(amount) AS total_amount
            FROM billing_transactions
            WHERE created_at >= :prevFrom
              AND created_at < :toExclusive
              AND status IN ('SUCCESS', 'FAILED', 'ROLLED_BACK')
            GROUP BY is_current, status, type, currency
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TransactionAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TransactionBucketRow> aggregateBuckets(
            LocalDateTime prevFromUtc,
            LocalDateTime curFromUtc,
            LocalDateTime toExclusiveUtc,
            int queryTimeoutSeconds
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("prevFrom", Timestamp.valueOf(prevFromUtc))
                .addValue("curFrom", Timestamp.valueOf(curFromUtc))
                .addValue("toExclusive", Timestamp.valueOf(toExclusiveUtc));

        jdbcTemplate.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);
        return jdbcTemplate.query(SQL, params, (rs, rowNum) -> new TransactionBucketRow(
                rs.getInt("is_current") == 1,
                rs.getString("status"),
                rs.getString("type"),
                rs.getString("currency"),
                rs.getLong("cnt"),
                rs.getBigDecimal("total_amount") != null ? rs.getBigDecimal("total_amount") : BigDecimal.ZERO
        ));
    }
}