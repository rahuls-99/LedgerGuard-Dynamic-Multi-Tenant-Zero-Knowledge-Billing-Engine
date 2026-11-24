package com.ledgerguard.analytics.repo;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class UsageAnalyticsRepository {

    public record UsageAggregateRow(
            String code,
            String name,
            String unit,
            BigDecimal currentQuantity,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            long currentRows
    ) {}

    private static final String SQL = """
            SELECT r.code,
                   r.name,
                   r.unit,
                   SUM(CASE WHEN d.usage_date >= :curFrom THEN d.quantity ELSE 0 END) AS cur_qty,
                   SUM(CASE WHEN d.usage_date >= :curFrom THEN d.amount   ELSE 0 END) AS cur_amt,
                   SUM(CASE WHEN d.usage_date <  :curFrom THEN d.amount   ELSE 0 END) AS prev_amt,
                   SUM(CASE WHEN d.usage_date >= :curFrom THEN 1        ELSE 0 END) AS cur_rows
            FROM usage_daily d
            JOIN resources r ON r.code = d.resource_code
            WHERE d.usage_date >= :prevFrom
              AND d.usage_date <= :toDate
            GROUP BY r.code, r.name, r.unit
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UsageAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UsageAggregateRow> aggregateUsage(
            LocalDate prevFromDate,
            LocalDate curFromDate,
            LocalDate toDate,
            int queryTimeoutSeconds
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("prevFrom", Date.valueOf(prevFromDate))
                .addValue("curFrom", Date.valueOf(curFromDate))
                .addValue("toDate", Date.valueOf(toDate));

        jdbcTemplate.getJdbcTemplate().setQueryTimeout(queryTimeoutSeconds);
        return jdbcTemplate.query(SQL, params, (rs, rowNum) -> new UsageAggregateRow(
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getBigDecimal("cur_qty") != null ? rs.getBigDecimal("cur_qty") : BigDecimal.ZERO,
                rs.getBigDecimal("cur_amt") != null ? rs.getBigDecimal("cur_amt") : BigDecimal.ZERO,
                rs.getBigDecimal("prev_amt") != null ? rs.getBigDecimal("prev_amt") : BigDecimal.ZERO,
                rs.getLong("cur_rows")
        ));
    }
}