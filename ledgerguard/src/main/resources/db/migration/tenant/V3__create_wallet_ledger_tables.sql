CREATE TABLE IF NOT EXISTS billing_wallets (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    is_system TINYINT(1) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_bw_balance CHECK (is_system = 1 OR balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS billing_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(64) NULL,
    request_hash CHAR(64) NOT NULL,
    type VARCHAR(10) NOT NULL,
    wallet_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(12) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_btx_transaction_id (transaction_id),
    UNIQUE KEY uq_btx_idempotency_key (idempotency_key),
    KEY idx_btx_wallet_created (wallet_id, created_at),
    KEY idx_btx_status_created (status, created_at),
    CONSTRAINT chk_btx_amount CHECK (amount > 0),
    CONSTRAINT fk_btx_wallet FOREIGN KEY (wallet_id) REFERENCES billing_wallets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS billing_ledger_entries (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entry_id VARCHAR(40) NOT NULL,
    transaction_id VARCHAR(40) NOT NULL,
    wallet_id VARCHAR(36) NOT NULL,
    direction VARCHAR(6) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    balance_after DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_ble_entry_id (entry_id),
    UNIQUE KEY uq_ble_tx_wallet_dir (transaction_id, wallet_id, direction),
    KEY idx_ble_wallet_created (wallet_id, created_at),
    CONSTRAINT chk_ble_amount CHECK (amount > 0),
    CONSTRAINT fk_ble_tx FOREIGN KEY (transaction_id) REFERENCES billing_transactions(transaction_id),
    CONSTRAINT fk_ble_wallet FOREIGN KEY (wallet_id) REFERENCES billing_wallets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO billing_wallets (id, owner_id, currency, balance, is_system)
VALUES
    ('sys-rev-inr', 'SYSTEM', 'INR', 0.00, 1),
    ('sys-fund-inr', 'SYSTEM', 'INR', 0.00, 1),
    ('sys-rev-usd', 'SYSTEM', 'USD', 0.00, 1),
    ('sys-fund-usd', 'SYSTEM', 'USD', 0.00, 1);