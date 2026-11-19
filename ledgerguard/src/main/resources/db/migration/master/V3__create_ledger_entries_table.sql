CREATE TABLE IF NOT EXISTS ledger_entries (
    -- 5. Primary Key
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Organization & Account Relationship
    org_id VARCHAR(100) NOT NULL,
    account_id BIGINT NOT NULL,

    -- 2 & 3. Transaction ID & Event ID (Idempotency / Duplicate Prevention)
    transaction_id VARCHAR(120) NOT NULL,
    event_id VARCHAR(120) NOT NULL,

    -- Entry Details & 7. Database Constraints
    entry_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'POSTED',
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 7. CHECK Constraints on Amount, Entry Type & Status
    CONSTRAINT chk_ledger_amount CHECK (amount > 0),
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_status CHECK (status IN ('PENDING', 'POSTED', 'REVERSED', 'FAILED')),

    -- 4. Unique Constraints / Indexes (Prevent duplicate transactions & events per org)
    CONSTRAINT uq_org_transaction_id UNIQUE (org_id, transaction_id),
    CONSTRAINT uq_org_event_id UNIQUE (org_id, event_id),

    -- 6. Foreign Key to Accounts table within tenant DB
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- 8. Composite Indexes for fast multi-tenant querying
    INDEX idx_ledger_org_tx (org_id, transaction_id),
    INDEX idx_ledger_org_account_status (org_id, account_id, status),
    INDEX idx_ledger_org_created (org_id, created_at)
);