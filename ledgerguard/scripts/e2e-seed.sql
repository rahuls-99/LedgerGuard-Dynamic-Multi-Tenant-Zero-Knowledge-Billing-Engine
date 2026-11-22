-- Seed wallets and 57 historical transactions for a tenant database.
-- Passwords for admin@org*.com and member@org*.com are injected via environment variables
-- in scripts/reset-e2e-data.sh (never committed in plain text).

DELETE FROM ledger_entries;
DELETE FROM transactions;
DELETE FROM wallets;

INSERT INTO wallets (id, owner_id, currency, balance, is_system) VALUES
  ('W-__PREFIX__-1001', 'cust-1001', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1002', 'cust-1002', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1003', 'cust-1003', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1004', 'cust-1004', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1005', 'cust-1005', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1006', 'cust-1006', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1007', 'cust-1007', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1008', 'cust-1008', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1009', 'cust-1009', 'INR', 1000.00, 0),
  ('W-__PREFIX__-1010', 'cust-1010', 'INR', 1000.00, 0),
  ('W-__PREFIX__-LOW',  'cust-low',  'INR',   50.00, 0),
  ('W-__PREFIX__-HIST', 'cust-hist', 'INR', 5000.00, 0),
  ('W-__PREFIX__-SYS-REV', 'system', 'INR',    0.00, 1);

-- 57 historical transactions on W-__PREFIX__-HIST (mixed SUCCESS, FAILED, ROLLED_BACK)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 57
)
INSERT INTO transactions (
  transaction_id, idempotency_key, request_hash, type, wallet_id, amount, currency, status, failure_reason, created_at, updated_at
)
SELECT
  CONCAT('TXN-HIST-__PREFIX__-', LPAD(n, 3, '0')),
  CONCAT('idem-hist-__PREFIX__-', n),
  SHA2(CONCAT('__PREFIX__-hist-', n), 256),
  'CHARGE',
  CONCAT('W-__PREFIX__-HIST'),
  10.00,
  'INR',
  CASE
    WHEN n % 5 = 0 THEN 'FAILED'
    WHEN n % 9 = 0 THEN 'ROLLED_BACK'
    ELSE 'SUCCESS'
  END,
  CASE
    WHEN n % 5 = 0 THEN 'INSUFFICIENT_FUNDS'
    WHEN n % 9 = 0 THEN 'TRANSACTION_ROLLED_BACK'
    ELSE NULL
  END,
  DATE_SUB(NOW(3), INTERVAL (120 - n) MINUTE),
  DATE_SUB(NOW(3), INTERVAL (120 - n) MINUTE)
FROM seq;