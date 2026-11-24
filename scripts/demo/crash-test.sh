#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKEND_DIR="$REPO_ROOT/ledgerguard"
EVIDENCE_DIR="$REPO_ROOT/docs/evidence"
DB_HOST="${DB_HOST:-127.0.0.1}"; DB_PORT="${DB_PORT:-3306}"; DB_USER="${DB_USER:-root}"; DB_PASS="${DB_PASS:-Rahul@2004}"
TENANT_DB="${TENANT_DB:-db_tenant_alpha}"; BASE_URL="${BASE_URL:-http://localhost:8080}"
JWT_SECRET="${JWT_SECRET:-LedgerGuardSuperSecretKeyForJwtSigning2026MustBeAtLeast32BytesLong!}"
mkdir -p "$EVIDENCE_DIR" "$BACKEND_DIR/.demo"
make_jwt() {
  local now exp hdr pay unsigned sig
  now=$(date +%s); exp=$((now + 86400))
  hdr=$(printf '{"alg":"HS512"}' | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  pay=$(printf '{"sub":"user-alpha-1","userId":"user-alpha-1","orgId":"alpha","role":"ADMIN","iat":%d,"exp":%d}' "$now" "$exp" | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  unsigned="${hdr}.${pay}"
  sig=$(printf '%s' "$unsigned" | openssl dgst -sha512 -hmac "$JWT_SECRET" -binary | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  printf '%s.%s' "$unsigned" "$sig"
}
stop_port_8080() {
  set +e
  local pids
  pids=$(netstat -ano 2>/dev/null | awk '/:8080 / && /LISTENING/ {print $5}' | sort -u)
  for p in $pids; do
    taskkill //F //PID "$p" >/dev/null 2>&1 || kill -9 "$p" >/dev/null 2>&1
  done
  set -e
}
cleanup() {
  stop_port_8080
  rm -f "$BACKEND_DIR/.demo/failure" "$BACKEND_DIR/.demo"/failure.claimed.* 2>/dev/null || true
}
trap cleanup EXIT
mysql_exec() {
  "/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" --batch --raw -e "$1" 2>/dev/null
}
wait_for_health() {
  for _ in $(seq 1 60); do
    if curl -sf "$BASE_URL/api/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "ERROR: Backend did not become healthy at $BASE_URL/api/health within 60s"
  return 1
}
start_backend() {
  stop_port_8080
  (cd "$BACKEND_DIR" && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo > "$EVIDENCE_DIR/backend-demo.log" 2>&1) &
  wait_for_health
}
run_invariants() {
  echo "--- SQL INVARIANTS CHECK ($TENANT_DB) ---"
  mysql_exec "
    SELECT '1_UNBALANCED_TX' AS chk, COUNT(*) AS violations FROM (
      SELECT transaction_id,
             SUM(CASE WHEN direction='DEBIT' THEN amount ELSE 0 END) AS d,
             SUM(CASE WHEN direction='CREDIT' THEN amount ELSE 0 END) AS c
      FROM $TENANT_DB.billing_ledger_entries GROUP BY transaction_id HAVING d <> c
    ) t;
    SELECT '2_NEGATIVE_BAL' AS chk, COUNT(*) AS violations FROM $TENANT_DB.billing_wallets WHERE is_system=0 AND balance<0;
    SELECT '3_ORPHAN_ENTRIES' AS chk, COUNT(*) AS violations FROM $TENANT_DB.billing_ledger_entries e JOIN $TENANT_DB.billing_transactions tx ON e.transaction_id=tx.transaction_id WHERE tx.status<>'SUCCESS';
  "
}
TOKEN=$(make_jwt)
for POINT in AFTER_DEBIT BEFORE_COMMIT; do
  OUT_FILE="$EVIDENCE_DIR/crash-${POINT}-$(date +%Y%m%d-%H%M%S).txt"
  WALLET_ID="w-crash-$(date +%s)"
  IDEMP_KEY="crash-demo-key-${POINT}-001"
  {
    echo "============================================================"
    echo "CRASH SIMULATION EVIDENCE: $POINT"
    echo "Timestamp: $(date -Iseconds)"
    echo "============================================================"
    rm -f "$BACKEND_DIR/.demo/failure" "$BACKEND_DIR/.demo"/failure.claimed.*
    start_backend
    curl -sf -X POST "$BASE_URL/api/v1/wallets" \
      -H "Authorization: Bearer $TOKEN" \
      -H "X-Tenant-ID: alpha" \
      -H "Content-Type: application/json" \
      -d "{\"walletId\":\"$WALLET_ID\",\"ownerId\":\"cust-crash\",\"currency\":\"INR\",\"initialBalance\":1000.00}" >/dev/null
    echo "[STEP 2] Starting balance of $WALLET_ID:"
    mysql_exec "SELECT id, balance, currency FROM $TENANT_DB.billing_wallets WHERE id='$WALLET_ID';"
    "$SCRIPT_DIR/arm-failure.sh" "${POINT}:CRASH"
    echo "[STEP 4] Sending charge of 250.00 with key=$IDEMP_KEY (expecting JVM halt 137)..."
    set +e
    curl -sS -w "\nHTTP_CODE:%{http_code}\nCURL_EXIT:%{exitcode}\n" \
      -X POST "$BASE_URL/api/v1/ledger/post" \
      -H "Authorization: Bearer $TOKEN" \
      -H "X-Tenant-ID: alpha" \
      -H "Idempotency-Key: $IDEMP_KEY" \
      -H "Content-Type: application/json" \
      -d "{\"type\":\"CHARGE\",\"walletId\":\"$WALLET_ID\",\"amount\":250.00,\"currency\":\"INR\"}" 2>&1
    set -e
    sleep 2
    echo "[STEP 5] MySQL state immediately after crash:"
    mysql_exec "SELECT transaction_id, status, amount, created_at FROM $TENANT_DB.billing_transactions WHERE wallet_id='$WALLET_ID' ORDER BY created_at DESC LIMIT 1;"
    mysql_exec "SELECT id, balance FROM $TENANT_DB.billing_wallets WHERE id='$WALLET_ID';"
    mysql_exec "SELECT COUNT(*) AS entry_count FROM $TENANT_DB.billing_ledger_entries WHERE wallet_id='$WALLET_ID';"
    echo "[STEP 6] Restarting backend in demo profile..."
    start_backend
    echo "[STEP 7] Polling retry with same key ($IDEMP_KEY) every 5s for 90s:"
    for SEC in $(seq 0 5 90); do
      RESP=$(curl -sS -i -X POST "$BASE_URL/api/v1/ledger/post" \
        -H "Authorization: Bearer $TOKEN" \
        -H "X-Tenant-ID: alpha" \
        -H "Idempotency-Key: $IDEMP_KEY" \
        -H "Content-Type: application/json" \
        -d "{\"type\":\"CHARGE\",\"walletId\":\"$WALLET_ID\",\"amount\":250.00,\"currency\":\"INR\"}" 2>&1 || true)
      echo "[t+${SEC}s] $(echo "$RESP" | head -n 1 | tr -d '\r') | $(echo "$RESP" | grep -i 'Idempotent-Replayed' | tr -d '\r' || true) | $(echo "$RESP" | tail -n 1)"
      sleep 5
    done
    echo "[STEP 8] Final DB state & Invariants after recovery:"
    mysql_exec "SELECT transaction_id, status, amount, failure_reason FROM $TENANT_DB.billing_transactions WHERE wallet_id='$WALLET_ID' ORDER BY created_at DESC LIMIT 1;"
    mysql_exec "SELECT id, balance FROM $TENANT_DB.billing_wallets WHERE id='$WALLET_ID';"
    run_invariants
  } | tee "$OUT_FILE"
  echo "Saved evidence -> $OUT_FILE"
done