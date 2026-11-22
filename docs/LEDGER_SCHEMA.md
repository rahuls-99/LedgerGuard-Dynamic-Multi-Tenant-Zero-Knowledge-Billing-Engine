# Append-Only Ledger Schema (LEDGER_SCHEMA.md)

## 1. Core Tables
### ledger_transactions
- Primary Key: id (BIGINT AUTO_INCREMENT)
- Business ID: transaction_id (VARCHAR(64), UNIQUE uk_txn_transaction_id)
- Tenant & Idempotency: (tenant_id, idempotency_key) (UNIQUE uk_txn_tenant_idempotency; multiple NULL keys permitted)
- Integrity: amount > 0 (DECIMAL(19,4)), request_hash for payload replay verification.
- Lifecycle: PENDING -> SUCCESS | FAILED, and SUCCESS -> ROLLED_BACK (terminal states cannot revert).

### ledger_entries (Strict Append-Only)
- Primary Key: id (BIGINT AUTO_INCREMENT)
- Entry ID: entry_id (VARCHAR(64), UNIQUE)
- Double-Entry Fields: transaction_id, wallet_id, direction (DEBIT | CREDIT), amount, balance_after.
- Immutability Rule: Rows are never updated or deleted; reversals append a compensating entry linked to a ROLLED_BACK transaction.

## 2. Transactional Atomicity
Wallet balance mutation, ledger_transactions state update, and ledger_entries insertion execute inside a single Spring @Transactional boundary under the wallet's Redis lock.
