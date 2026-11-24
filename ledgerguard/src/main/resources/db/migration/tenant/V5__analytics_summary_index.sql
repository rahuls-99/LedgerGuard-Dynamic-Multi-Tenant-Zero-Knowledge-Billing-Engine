CREATE INDEX idx_tx_analytics
    ON transactions (created_at, status, type, currency, amount);

DROP INDEX idx_tx_created_status_type_amount
    ON transactions;