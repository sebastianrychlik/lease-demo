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

PL_CONTAINER="lease-demo-postgres-pl"
DE_CONTAINER="lease-demo-postgres-de"
DB_NAME="lease_demo"
DB_USER="leasedemo"

for container in "$PL_CONTAINER" "$DE_CONTAINER"; do

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
# Spock replication safety (M5.1.3.2)
#
# lease_product is already in the Spock replication set. Flyway schema
# changes (e.g. V4 adding lease_product.settlement_currency) are NOT
# replicated by Spock, so replicated DML referencing a new column must
# never reach a node whose schema doesn't have it yet.
#
# Strategy: disable PL<->DE subscriptions -> run Flyway on both databases
# -> only if BOTH succeed, re-enable subscriptions and verify state. On
# failure, subscriptions are LEFT DISABLED and the failure/recovery action
# is printed clearly. Nodes/subscriptions/replication sets are never
# dropped or rebuilt here.
# ---------------------------------------------------------------------------

get_subscriptions() {
    local container="$1"
    docker exec "$container" \
        psql -U "$DB_USER" -d "$DB_NAME" -tAc \
        "SELECT sub_name FROM spock.subscription;" 2>/dev/null || true
}

subscription_enabled() {
    local container="$1"
    local sub_name="$2"
    docker exec "$container" \
        psql -U "$DB_USER" -d "$DB_NAME" -tAc \
        "SELECT sub_enabled FROM spock.subscription WHERE sub_name = '$sub_name';" 2>/dev/null || true
}

mapfile -t PL_SUBS < <(get_subscriptions "$PL_CONTAINER")
mapfile -t DE_SUBS < <(get_subscriptions "$DE_CONTAINER")

echo "Detected Spock subscriptions:"
echo "  PL: ${PL_SUBS[*]:-<none>}"
echo "  DE: ${DE_SUBS[*]:-<none>}"
echo

DISABLED_PL_SUBS=()
DISABLED_DE_SUBS=()

disable_subscriptions() {
    echo "----------------------------------------"
    echo " Disabling replication subscriptions"
    echo "----------------------------------------"

    for sub in "${PL_SUBS[@]}"; do
        [ -z "$sub" ] && continue
        ENABLED="$(subscription_enabled "$PL_CONTAINER" "$sub")"
        if [ "$ENABLED" = "t" ]; then
            echo "PL: disabling subscription '$sub'"
            docker exec "$PL_CONTAINER" \
                psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
                -c "SELECT spock.sub_disable('$sub', true);"
            DISABLED_PL_SUBS+=("$sub")
        else
            echo "PL: subscription '$sub' already disabled — skip"
        fi
    done

    for sub in "${DE_SUBS[@]}"; do
        [ -z "$sub" ] && continue
        ENABLED="$(subscription_enabled "$DE_CONTAINER" "$sub")"
        if [ "$ENABLED" = "t" ]; then
            echo "DE: disabling subscription '$sub'"
            docker exec "$DE_CONTAINER" \
                psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
                -c "SELECT spock.sub_disable('$sub', true);"
            DISABLED_DE_SUBS+=("$sub")
        else
            echo "DE: subscription '$sub' already disabled — skip"
        fi
    done
    echo
}

enable_subscriptions() {
    echo "----------------------------------------"
    echo " Re-enabling replication subscriptions"
    echo "----------------------------------------"

    for sub in "${DISABLED_PL_SUBS[@]}"; do
        echo "PL: enabling subscription '$sub'"
        docker exec "$PL_CONTAINER" \
            psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
            -c "SELECT spock.sub_enable('$sub', true);"
    done

    for sub in "${DISABLED_DE_SUBS[@]}"; do
        echo "DE: enabling subscription '$sub'"
        docker exec "$DE_CONTAINER" \
            psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
            -c "SELECT spock.sub_enable('$sub', true);"
    done
    echo
}

print_subscription_state() {
    echo "----------------------------------------"
    echo " Current subscription state"
    echo "----------------------------------------"
    echo "PL:"
    docker exec "$PL_CONTAINER" \
        psql -U "$DB_USER" -d "$DB_NAME" \
        -c "SELECT sub_name, sub_enabled FROM spock.subscription;" 2>/dev/null || true
    echo "DE:"
    docker exec "$DE_CONTAINER" \
        psql -U "$DB_USER" -d "$DB_NAME" \
        -c "SELECT sub_name, sub_enabled FROM spock.subscription;" 2>/dev/null || true
}

disable_subscriptions

# ---------------------------------------------------------------------------
# Flyway migrations — run independently against PL and DE
# ---------------------------------------------------------------------------

echo "Running Flyway migrations..."
echo

cd "$BACKEND_DIR"

MIGRATION_OK=1
mvn -Plocal-db-migrate initialize || MIGRATION_OK=0

cd "$ROOT_DIR"

if [ "$MIGRATION_OK" -eq 0 ]; then
    echo
    echo "========================================"
    echo " ERROR: Database migration FAILED"
    echo "========================================"
    echo
    echo "Flyway migration failed against PL and/or DE (see Maven output above)."
    echo "Replication subscriptions are LEFT DISABLED — re-enabling now could"
    echo "replicate DML against asymmetric schemas."
    echo
    print_subscription_state
    echo
    echo "Recovery action required:"
    echo "  1. Fix the failing migration."
    echo "  2. Re-run ./scripts/database-migrate.sh so both PL and DE succeed"
    echo "     before subscriptions are re-enabled."
    exit 1
fi

# ---------------------------------------------------------------------------
# Both migrations succeeded — safe to re-enable replication
# ---------------------------------------------------------------------------

enable_subscriptions
print_subscription_state

echo
echo "========================================"
echo " Database migration completed"
echo "========================================"
echo
echo "PostgreSQL PL schema: up to date"
echo "PostgreSQL DE schema: up to date"
echo "Replication subscriptions: re-enabled"
