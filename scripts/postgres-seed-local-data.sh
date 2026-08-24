#!/usr/bin/env bash
# =============================================================================
# scripts/postgres-seed-local-data.sh — lease-demo
#
# LOCAL-DEVELOPMENT-ONLY. Populates the local PostgreSQL database with
# realistic synthetic Customer records for development/demo purposes.
#
# Flow:
#   1. Generate mock Customer business data with the Python generator
#      (PESEL validated/checksummed locally as a fast fail-safe).
#   2. Invoke the Spring Boot local seeding mechanism (CustomerSeedRunner)
#      via the explicit "local-seed" Maven profile, which
#      re-validates/encrypts/hashes/persists each record through the
#      real CustomerService business path.
#   3. Report Requested/Created/Skipped/Failed counts.
#   4. Clean up the temporary plaintext seed file.
#
# CustomerSeedRunner/CustomerSeedProperties/CustomerSeedRecord live under
# backend/src/local-seed/{java,test} and are ONLY compiled/packaged when
# the Maven "local-seed" profile is explicitly active (see backend/pom.xml).
# A normal `mvn clean package`/`mvn clean verify` never includes them —
# this script is the only supported way to opt into local seeding.
#
# Usage:
#   ./scripts/postgres-seed-local-data.sh          # 50 customers (default)
#   ./scripts/postgres-seed-local-data.sh 100      # custom count
#
# This script does NOT reset/drop the database — see postgres-reset.sh for
# that separate operation. It does NOT print PESEL values or crypto keys.
# =============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd -W 2>/dev/null || pwd)"
COUNT="${1:-50}"

TMP_DIR="$ROOT_DIR/tmp"
SEED_FILE="$TMP_DIR/mock-customers.json"

echo "=== LeaseDemo local Customer seeder ==="
echo "Requested customer count: $COUNT"
echo

# ---------------------------------------------------------------------------
# 1. Prerequisites
# ---------------------------------------------------------------------------
command -v python >/dev/null 2>&1 || command -v python3 >/dev/null 2>&1 || {
  echo "ERROR: python (or python3) is required but was not found on PATH." >&2
  exit 1
}
PYTHON_BIN="$(command -v python || command -v python3)"

command -v mvn >/dev/null 2>&1 || {
  echo "ERROR: mvn (Maven) is required but was not found on PATH." >&2
  exit 1
}

# Load the same persistent local crypto keys used by deploy-local.sh.
# This works from a fresh shell — no manual export required.
# shellcheck source=lib/local-crypto-keys.sh
source "$ROOT_DIR/scripts/lib/local-crypto-keys.sh"
load_local_crypto_keys "$ROOT_DIR"

mkdir -p "$TMP_DIR"

cleanup() {
  if [ -f "$SEED_FILE" ]; then
    rm -f "$SEED_FILE"
  fi
}
trap cleanup EXIT

# ---------------------------------------------------------------------------
# 2. Generate mock customer JSON (never prints PESEL values)
# ---------------------------------------------------------------------------
echo "Generating $COUNT mock customers..."
"$PYTHON_BIN" "$ROOT_DIR/scripts/generate-customer-mock-data.py" \
  --count "$COUNT" \
  --output "$SEED_FILE"

# ---------------------------------------------------------------------------
# 3. Invoke the local Spring seeding mechanism
# ---------------------------------------------------------------------------
echo "Importing mock customers via CustomerService (local-seed profile, local Spring profile)..."
(
  cd "$ROOT_DIR/backend"
  mvn -q -Plocal-seed spring-boot:run \
    -Dspring-boot.run.profiles=local \
    -Dspring-boot.run.arguments="--application.seed.customers.enabled=true --application.seed.customers.file=$SEED_FILE --server.port=0"
)

echo
echo "Done. See the application log output above for the"
echo "Requested/Created/Skipped/Failed summary."
echo "Temporary mock data file will now be removed: $SEED_FILE"

