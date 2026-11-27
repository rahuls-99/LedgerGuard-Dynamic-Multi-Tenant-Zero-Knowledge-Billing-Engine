#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TENANT="orgA"
COUNT=20
EVERY=3

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tenant) TENANT="$2"; shift 2 ;;
    --count) COUNT="$2"; shift 2 ;;
    --every) EVERY="$2"; shift 2 ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

PASS_VAR="DEMO_PASS_${TENANT}"
PASSWORD="${!PASS_VAR:-${DEMO_PASSWORD:-}}"
EMAIL_VAR="DEMO_EMAIL_${TENANT}"
EMAIL="${!EMAIL_VAR:-admin@orga.com}"

if [[ -z "$PASSWORD" ]]; then
  echo "Missing password env var (${PASS_VAR} or DEMO_PASSWORD)" >&2
  exit 1
fi

TOKEN=$(curl -sS -X POST "${BASE_URL}/api/real-auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"orgId\":\"${TENANT}\",\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}" | \
  node -e "const d = JSON.parse(require('fs').readFileSync(0,'utf8')); console.log(d.token ?? '');")

if [[ -z "$TOKEN" ]]; then
  echo "Failed to acquire JWT token" >&2
  exit 1
fi

for ((i = 1; i <= COUNT; i++)); do
  IDEMPOTENCY_KEY="idem-${TENANT}-$(date +%s)-${i}-$RANDOM"
  AMOUNT="10.00"
  if (( i % 5 == 0 )); then
    AMOUNT="9999999.00"
  fi
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/api/ledger/charges" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "X-Tenant-ID: ${TENANT}" \
    -H "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
    -H "Content-Type: application/json" \
    -d "{\"amount\":${AMOUNT},\"currency\":\"USD\"}")
  echo "[$i/$COUNT] amount=${AMOUNT} -> HTTP${CODE}"
  if (( i < COUNT )); then
    sleep "$EVERY"
  fi
done