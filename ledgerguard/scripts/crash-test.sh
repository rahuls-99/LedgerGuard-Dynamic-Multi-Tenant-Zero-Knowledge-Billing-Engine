#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKEND_DIR="$REPO_ROOT/ledgerguard"
EVIDENCE_DIR="$REPO_ROOT/docs/evidence"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-Rahul@2004}"
TENANT_DB="${TENANT_DB:-db_tenant_alpha}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

mkdir -p "$EVIDENCE_DIR" "$BACKEND_DIR/.demo"

BACKEND_PID=""
cleanup() {
  if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill -9 "$BACKEND_PID" 2>/dev/null || true
  fi
  rm -f "$BACKEND_DIR/.demo/failure" "$BACKEND_DIR/.demo"/failure.claimed.* 2>/dev/null || true
}
trap cleanup EXIT

mysql_exec() {
  mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" --batch --raw -e "$1" 2>/dev/null
}

wait_for_health() {
  for _ in $(seq 1 45); do
    if curl -sf "$BASE_URL/actuator/health" | grep -q '"UP"'; then
      return 0
    fi
    sleep 1
  done
  echo "ERROR: Backend failed to become healthy within 45s"
  return 1
}

start_backend() {
  (
    cd "$BACKEND_DIR"
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo > "$EVIDENCE_DIR/backend-demo.log" 2>&1
  ) &
  BACKEND_PID=$!
  wait_for_health
}

run_invariants() {
  echo "--- SQL INVARIANTS CHECK ($TENANT_DB) ---"
  mysql_exec "
    SELECT '1_UNBALANCED_TX' AS check_name, COUNT(*) AS violations
    FROM (
      SELECT transaction_id,
             SUM(CASE WHEN direction='DEBIT' THEN amount ELSE 0 END) AS debits,
             SUM(CASE WHEN direction='CREDIT' THEN amount ELSE 0 END) AS credits
      FROM $TENANT_DB.billing_ledger_entries
      GROUP BY transaction_id
      HAVING debits <> credits
    ) t;

    SELECT '2_NEGATIVE_CUSTOMER_BALANCE' AS check_name, COUNT(*) AS violations
    FROM $TENANT_DB.billing_wallets
    WHERE is_system = 0 AND balance < 0;

    SELECT '3_ENTRIES_FOR_NON_SUCCESS_TX' AS check_name, COUNT(*) AS violations
    FROM $TENANT_DB.billing_ledger_entries e
    JOIN $TENANT_DB.billing_transactions tx ON e.transaction_id = tx.transaction_id
    WHERE tx.status <> 'SUCCESS';
  "
}

POINTS=("AFTER_PENDING_INSERT" "AFTER_DEBIT" "AFTER_COMMIT")

for POINT in "${POINTS[@]}"; do
  TS="$(date +%Y%m%d-%H%M%S)"
  OUT_FILE="$EVIDENCE_DIR/crash-${POINT}-${TS}.txt"
  WALLET_ID="w-crash-$(date +%s)"
  IDEMP_KEY="crash-demo-key-${POINT}-001"

  {
    echo "============================================================"
    echo "CRASH SIMULATION EVIDENCE: $POINT"
    echo "Timestamp: $(date -Iseconds)"
    echo "============================================================"

    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
      kill -9 "$BACKEND_PID" 2>/dev/null || true
      BACKEND_PID=""
    fi
    rm -f "$BACKEND_DIR/.demo/failure" "$BACKEND_DIR/.demo"/failure.claimed.*

    # 1. Start backend with demo profile and seed wallet with 1000.00 INR
    start_backend
    curl -sf -X POST "$BASE_URL/api/v1/wallets" \
      -H "X-Tenant-ID: alpha" \
      -H "Content-Type: application/json" \
      -d "{\"walletId\":\"$WALLET_ID\",\"ownerId\":\"cust-crash\",\"currency\":\"INR\",\"initialBalance\":1000.00}" >/dev/null

    echo "[STEP 2] Starting balance of $WALLET_ID:"
    mysql_exec "SELECT id, balance, currency FROM $TENANT_DB.billing_wallets WHERE id='$WALLET_ID';"

    # 3. Arm failure hook
    "$SCRIPT_DIR/arm-failure.sh" "${POINT}:CRASH"

    # 4. Send 250.00 charge with fixed Idempotency-Key (expect connection reset / empty reply)
    echo "[STEP 4] Sending charge of 250.00 with key=$IDEMP_KEY (expecting JVM halt 137)..."
    set +e
    CURL_OUT=$(curl -sS -w "\nHTTP_CODE:%{http_code}\nCURL_EXIT:%{exitcode}\n" \
      -X POST "$BASE_URL/api/v1/ledger/post" \
      -H "X-Tenant-ID: alpha" \
      -H "Idempotency-Key: $IDEMP_KEY" \
      -H "Content-Type: application/json" \
      -d "{\"type\":\"CHARGE\",\"walletId\":\"$WALLET_ID\",\"amount\":250.00,\"currency\":\"INR\"}" 2>&1)
    set -e
    echo "$CURL_OUT"
    sleep 2

    # 5. Query MySQL right after crash
    echo "[STEP 5] MySQL state immediately after crash:"
    mysql_exec "SELECT transaction_id, status, amount, created_at FROM $TENANT_DB.billing_transactions WHERE wallet_id='$WALLET_ID' ORDER BY created_at DESC LIMIT 1;"
    mysql_exec "SELECT id, balance FROM $TENANT_DB.billing_wallets WHERE id='$WALLET_ID';"
    mysql_exec "SELECT COUNT(*) AS entry_count FROM $TENANT_DB.billing_ledger_entries WHERE wallet_id='$WALLET_ID';"

    # 6. Restart backend
    echo "[STEP 6] Restarting backend in demo profile..."
    start_backend

    # 7. Retry same Idempotency-Key every 5s for 90s to capture lock expiry & reaper recovery
    echo "[STEP 7] Polling retry with same key ($IDEMP_KEY) every 5s for 90s:"
    for SEC in $(seq 0 5 90); do
      RESP=$(curl -sS -i -X POST "$BASE_URL/api/v1/ledger/post" \
        -H "X-Tenant-ID: alpha" \
        -H "Idempotency-Key: $IDEMP_KEY" \
        -H "Content-Type: application/json" \
        -d "{\"type\":\"CHARGE\",\"walletId\":\"$WALLET_ID\",\"amount\":250.00,\"currency\":\"INR\"}" 2>&1 || true)
      STATUS_LINE=$(echo "$RESP" | head -n 1 | tr -d '\r')
      REPLAY_HDR=$(echo "$RESP" | grep -i "Idempotent-Replayed" | tr -d '\r' || true)
      BODY_LINE=$(echo "$RESP" | tail -n 1)
      echo "[t+${SEC}s] $STATUS_LINE | $REPLAY_HDR | $BODY_LINE"
      sleep 5
    done

    # 8. Final DB state & Invariants check
    echo "[STEP 8] Final DB state & Invariants after recovery:"
    mysql_exec "SELECT transaction_id, status, amount, failure_reason FROM $TENANT_DB.billing_transactions WHERE wallet_id='$WALLET_ID' ORDER BY created_at DESC LIMIT 1;"
    mysql_exec "SELECT id, balance FROM $TENANT_DB.billing_wallets WHERE id='$WALLET_ID';"
    run_invariants

  } | tee "$OUT_FILE"

  echo "Saved evidence -> $OUT_FILE"
done