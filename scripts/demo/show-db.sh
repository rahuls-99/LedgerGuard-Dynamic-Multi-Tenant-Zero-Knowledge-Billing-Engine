#!/usr/bin/env bash
set -euo pipefail
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-Rahul@2004}"
echo "=== TENANT DATABASES ==="
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -t -e "
SHOW DATABASES LIKE 'db\_tenant\_%';
SELECT 'alpha' AS tenant, COUNT(*) AS tx_count FROM db_tenant_alpha.billing_transactions
UNION ALL
SELECT 'beta' AS tenant, COUNT(*) AS tx_count FROM db_tenant_beta.billing_transactions;
SELECT 'alpha' AS tenant, id, balance, currency FROM db_tenant_alpha.billing_wallets LIMIT 5;
" 2>/dev/null