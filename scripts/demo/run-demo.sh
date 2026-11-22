#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
docker compose -f "$REPO_ROOT/docker-compose.yml" up -d
(cd "$REPO_ROOT/ledgerguard" && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo) &
PID=$!; trap 'kill -9 "$PID" 2>/dev/null || true' EXIT
for _ in $(seq 1 60); do curl -sf "http://localhost:8080/api/health" >/dev/null 2>&1 && break; sleep 1; done
"$SCRIPT_DIR/show-db.sh" || true
wait "$PID"