-- 1. idempotency_key कॉलम जोड़ें (NOT NULL constraint के साथ)
ALTER TABLE ledger_entries
    MODIFY COLUMN transaction_id VARCHAR(100) NOT NULL,
    ADD COLUMN idempotency_key VARCHAR(150) NOT NULL AFTER transaction_id;

-- 2. transaction_id पर Unique Index लागू करें
CREATE UNIQUE INDEX uk_transaction_id
    ON ledger_entries(transaction_id);

-- 3. org_id + idempotency_key पर Composite Unique Index लगाएं (Multi-Tenant Idempotency)
CREATE UNIQUE INDEX uk_org_idempotency
    ON ledger_entries(org_id, idempotency_key);