#!/usr/bin/env bash
set -euo pipefail
if [ $# -ne 1 ]; then
  echo "Usage: $0 <POINT>:<ACTION>"
  echo "  POINT : AFTER_PENDING_INSERT | AFTER_DEBIT | AFTER_COMMIT"
  echo "  ACTION: CRASH | THROW"
  exit 1
fi
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEMO_DIR="$REPO_ROOT/ledgerguard/.demo"
mkdir -p "$DEMO_DIR"
rm -f "$DEMO_DIR"/failure.claimed.*
printf '%s\n' "$1" > "$DEMO_DIR/failure"
echo "Armed one-shot failure hook: $1 ->$DEMO_DIR/failure"