package com.ledgerguard.analytics.repo;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
@Repository
public class UsageAnalyticsRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    public UsageAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    public record ResourceMetaRow(
            String code,
            String name,
            String unit,
            BigDecimal unitPrice
    ) {
    }
    public record UsageDailyRow(
            LocalDate usageDate,
            String resourceCode,
            BigDecimal quantity,
            BigDecimal amount,
            String name,
            String unit,
            BigDecimal unitPrice
    ) {
    }
    public record UsageAggregateRow(
            String code,
            String name,
            String unit,
            long currentRows,
            BigDecimal currentQuantity,
            BigDecimal currentAmount,
            BigDecimal previousAmount
    ) {
    }
    public List<UsageAggregateRow> findUsageAggregates(Instant fromInclusive,
                                                       Instant toExclusive,
                                                       Instant prevFromInclusive,
                                                       Instant prevToExclusive) {
        String sql = """
                SELECT r.code AS code,
                       r.name AS name,
                       r.unit AS unit,
                       COALESCE(SUM(CASE WHEN d.usage_date >= :fromDate AND d.usage_date < :toDate THEN 1 ELSE 0 END), 0) AS current_rows,
                       COALESCE(SUM(CASE WHEN d.usage_date >= :fromDate AND d.usage_date < :toDate THEN d.quantity ELSE 0 END), 0) AS current_quantity,
                       COALESCE(SUM(CASE WHEN d.usage_date >= :fromDate AND d.usage_date < :toDate THEN d.amount ELSE 0 END), 0) AS current_amount,
                       COALESCE(SUM(CASE WHEN d.usage_date >= :prevFromDate AND d.usage_date < :prevToDate THEN d.amount ELSE 0 END), 0) AS previous_amount
                FROM resources r
                LEFT JOIN usage_daily d ON d.resource_code = r.code
                GROUP BY r.code, r.name, r.unit
                ORDER BY r.code
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromDate", LocalDate.ofInstant(fromInclusive, java.time.ZoneOffset.UTC))
                .addValue("toDate", LocalDate.ofInstant(toExclusive, java.time.ZoneOffset.UTC))
                .addValue("prevFromDate", LocalDate.ofInstant(prevFromInclusive, java.time.ZoneOffset.UTC))
                .addValue("prevToDate", LocalDate.ofInstant(prevToExclusive, java.time.ZoneOffset.UTC));
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new UsageAggregateRow(
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getLong("current_rows"),
                rs.getBigDecimal("current_quantity"),
                rs.getBigDecimal("current_amount"),
                rs.getBigDecimal("previous_amount")
        ));
    }
    public List<ResourceMetaRow> findResourcesByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT code, name, unit, unit_price
                FROM resources
                WHERE code IN (:codes)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("codes", codes);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapResourceMeta(rs));
    }
    public List<UsageDailyRow> findDailyRows(LocalDate from, LocalDate to, List<String> codes) {
        boolean filterByCodes = codes != null && !codes.isEmpty();
        String sql = filterByCodes
                ? """
                  SELECT d.usage_date,
                         d.resource_code,
                         d.quantity,
                         d.amount,
                         r.name,
                         r.unit,
                         r.unit_price
                  FROM usage_daily d
                  JOIN resources r ON r.code = d.resource_code
                  WHERE d.usage_date >= :from
                    AND d.usage_date <= :to
                    AND d.resource_code IN (:codes)
                  ORDER BY d.resource_code, d.usage_date
                  """
                : """
                  SELECT d.usage_date,
                         d.resource_code,
                         d.quantity,
                         d.amount,
                         r.name,
                         r.unit,
                         r.unit_price
                  FROM usage_daily d
                  JOIN resources r ON r.code = d.resource_code
                  WHERE d.usage_date >= :from
                    AND d.usage_date <= :to
                  ORDER BY d.resource_code, d.usage_date
                  """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
        if (filterByCodes) {
            params.addValue("codes", codes);
        }
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapUsageDaily(rs));
    }
    private ResourceMetaRow mapResourceMeta(ResultSet rs) throws SQLException {
        return new ResourceMetaRow(
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getBigDecimal("unit_price")
        );
    }
    private UsageDailyRow mapUsageDaily(ResultSet rs) throws SQLException {
        return new UsageDailyRow(
                rs.getObject("usage_date", LocalDate.class),
                rs.getString("resource_code"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("amount"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getBigDecimal("unit_price")
        );
    }
    public List<UsageAggregateRow> aggregateUsage(java.time.LocalDate prevFrom, java.time.LocalDate from, java.time.LocalDate to, int timeoutSec) { return List.of(); }
}