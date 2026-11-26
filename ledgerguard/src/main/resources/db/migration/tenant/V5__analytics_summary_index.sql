SET @idx_exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'billing_transactions' AND index_name = 'idx_tx_analytics_window_status');
SET @ddl := IF(@idx_exists = 0, 'CREATE INDEX idx_tx_analytics_window_status ON billing_transactions (created_at, status, type, currency, amount)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
