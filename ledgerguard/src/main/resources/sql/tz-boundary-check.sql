SELECT @@global.time_zone, @@session.time_zone;

INSERT INTO transactions (transaction_id, wallet_id, type, status, currency, amount, created_at)
VALUES
  ('TXN-TZ-BOUNDARY-1', 'W-A-OLD-1', 'CHARGE', 'SUCCESS', 'USD', 10.00, '2026-11-19 23:59:59.999'),
  ('TXN-TZ-BOUNDARY-2', 'W-A-OLD-1', 'CHARGE', 'SUCCESS', 'USD', 20.00, '2026-11-20 00:00:00.000');

SELECT transaction_id, created_at, DATE(created_at) AS utc_day
FROM transactions
WHERE transaction_id IN ('TXN-TZ-BOUNDARY-1', 'TXN-TZ-BOUNDARY-2');