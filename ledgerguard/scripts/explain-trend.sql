ANALYZE TABLE transactions;

EXPLAIN
SELECT DATE(created_at) AS d,
       status,
       type,
       currency,
       COUNT(*) AS cnt,
       SUM(amount) AS amt
FROM transactions
WHERE created_at >= '2025-11-27 00:00:00'
  AND created_at <  '2026-11-27 00:00:00'
  AND status IN ('SUCCESS', 'FAILED', 'ROLLED_BACK')
GROUP BY d, status, type, currency;

EXPLAIN ANALYZE
SELECT DATE(created_at) AS d,
       status,
       type,
       currency,
       COUNT(*) AS cnt,
       SUM(amount) AS amt
FROM transactions
WHERE created_at >= '2025-11-27 00:00:00'
  AND created_at <  '2026-11-27 00:00:00'
  AND status IN ('SUCCESS', 'FAILED', 'ROLLED_BACK')
GROUP BY d, status, type, currency;