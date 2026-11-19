CREATE TABLE IF NOT EXISTS ledger_accounts (
    id VARCHAR(64) PRIMARY KEY,
    account_code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS journal_entries (
    id VARCHAR(64) PRIMARY KEY,
    journal_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    reference_id VARCHAR(100),
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_journal_id (journal_id),
    INDEX idx_account_id (account_id),
    CONSTRAINT fk_journal_account FOREIGN KEY (account_id) REFERENCES ledger_accounts(id)
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    organization_id VARCHAR(64),
    transaction_id VARCHAR(64),
    request_hash VARCHAR(64) NOT NULL DEFAULT 'NONE',
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    http_status INT DEFAULT 200,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO ledger_accounts (id, account_code, name, account_type, currency, balance)
VALUES
    ('acc_cash_01', '1001', 'Operating Cash Account', 'ASSET', 'INR', 100000.0000),
    ('acc_revenue_01', '4001', 'Client Revenue Account', 'REVENUE', 'INR', 0.0000),
    ('acc_expense_01', '5001', 'Operating Expense Account', 'EXPENSE', 'INR', 0.0000);