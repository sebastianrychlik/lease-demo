#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"

echo "========================================"
echo " LeaseDemo — Database Migration"
echo "========================================"
echo
echo "Migration source:"
echo "  backend/src/main/resources/db/migration"
echo
echo "Targets:"
echo "  PL -> localhost:5433/lease_demo"
echo "  DE -> localhost:5434/lease_demo"
echo

# ---------------------------------------------------------------------------
# Preconditions
# ---------------------------------------------------------------------------

for container in lease-demo-postgres-pl lease-demo-postgres-de; do

    if ! docker inspect "$container" >/dev/null 2>&1; then
        echo "ERROR: Container does not exist:"
        echo "  $container"
        echo
        echo "Start infrastructure first:"
        echo "  ./scripts/infrastructure-start.sh"
        exit 1
    fi

    STATUS="$(docker inspect -f '{{.State.Status}}' "$container")"

    if [ "$STATUS" != "running" ]; then
        echo "ERROR: Container is not running:"
        echo "  $container ($STATUS)"
        echo
        echo "Start infrastructure first:"
        echo "  ./scripts/infrastructure-start.sh"
        exit 1
    fi

done

# ---------------------------------------------------------------------------
# Flyway migrations
# ---------------------------------------------------------------------------

echo "Running Flyway migrations..."
echo

cd "$BACKEND_DIR"

mvn -Plocal-db-migrate initialize

echo
echo "========================================"
echo " Database migration completed"
echo "========================================"
echo
echo "PostgreSQL PL schema: up to date"
echo "PostgreSQL DE schema: up to date"
