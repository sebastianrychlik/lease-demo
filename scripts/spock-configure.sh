#!/usr/bin/env bash

set -euo pipefail

PL_CONTAINER="lease-demo-postgres-pl"
DE_CONTAINER="lease-demo-postgres-de"

DB_NAME="lease_demo"
DB_USER="leasedemo"

REPLICATION_SET="default"

# ---------------------------------------------------------------------------
# Tables replicated by Spock
#
# Add business tables here when they should participate in PL <-> DE
# replication.
#
# DO NOT add:
#   - flyway_schema_history
#   - Spock internal tables
#
# outbox_events will be handled deliberately later because of Kafka/Debezium.
# ---------------------------------------------------------------------------

REPLICATED_TABLES=(
    "public.customers"
    "public.replication_demo"
)

echo "========================================"
echo " LeaseDemo — Spock Configuration"
echo "========================================"
echo
echo "Replication set:"
echo "  $REPLICATION_SET"
echo
echo "Nodes:"
echo "  PL -> $PL_CONTAINER"
echo "  DE -> $DE_CONTAINER"
echo

# ---------------------------------------------------------------------------
# Preconditions
# ---------------------------------------------------------------------------

for container in "$PL_CONTAINER" "$DE_CONTAINER"; do

    if ! docker inspect "$container" >/dev/null 2>&1; then
        echo "ERROR: Container does not exist:"
        echo "  $container"
        exit 1
    fi

    STATUS="$(docker inspect -f '{{.State.Status}}' "$container")"

    if [ "$STATUS" != "running" ]; then
        echo "ERROR: Container is not running:"
        echo "  $container ($STATUS)"
        exit 1
    fi

done

# ---------------------------------------------------------------------------
# Configure replication tables on both nodes
#
# Idempotent:
#   - table missing in DB        -> ERROR
#   - already in replication set -> SKIP
#   - not yet configured         -> ADD
# ---------------------------------------------------------------------------

for container in "$PL_CONTAINER" "$DE_CONTAINER"; do

    echo "----------------------------------------"
    echo "Configuring: $container"
    echo "----------------------------------------"

    for table in "${REPLICATED_TABLES[@]}"; do

        echo
        echo "Table: $table"

        SCHEMA_NAME="${table%%.*}"
        TABLE_NAME="${table#*.}"

        # -------------------------------------------------------------------
        # 1. Make sure Flyway has already created the table
        # -------------------------------------------------------------------

        EXISTS="$(
            docker exec "$container" \
                psql \
                -U "$DB_USER" \
                -d "$DB_NAME" \
                -tAc "
                    SELECT to_regclass('$table') IS NOT NULL;
                "
        )"

        if [ "$EXISTS" != "t" ]; then
            echo "ERROR: Table does not exist:"
            echo "  $table"
            echo
            echo "Run database migrations first:"
            echo "  ./scripts/database-migrate.sh"
            exit 1
        fi

        # -------------------------------------------------------------------
        # 2. Check whether table is already in the replication set
        # -------------------------------------------------------------------

        IS_REPLICATED="$(
            docker exec "$container" \
                psql \
                -U "$DB_USER" \
                -d "$DB_NAME" \
                -tAc "
                    SELECT EXISTS (
                        SELECT 1
                        FROM spock.tables
                        WHERE nspname = '$SCHEMA_NAME'
                          AND relname = '$TABLE_NAME'
                          AND set_name = '$REPLICATION_SET'
                    );
                "
        )"

        if [ "$IS_REPLICATED" = "t" ]; then
            echo "  ALREADY CONFIGURED"
            continue
        fi

        # -------------------------------------------------------------------
        # 3. Table exists but is not replicated -> add it
        # -------------------------------------------------------------------

        docker exec "$container" \
            psql \
            -U "$DB_USER" \
            -d "$DB_NAME" \
            -v ON_ERROR_STOP=1 \
            -c "
                SELECT spock.repset_add_table(
                    '$REPLICATION_SET',
                    '$table',
                    synchronize_data := false
                );
            "

        echo "  ADDED"

    done

done

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------

echo
echo "========================================"
echo " PostgreSQL PL replication tables"
echo "========================================"

docker exec "$PL_CONTAINER" \
    psql \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -c "
        SELECT
            nspname AS schema,
            relname AS table_name,
            set_name AS replication_set
        FROM spock.tables
        WHERE set_name IS NOT NULL
        ORDER BY nspname, relname, set_name;
    "

echo
echo "========================================"
echo " PostgreSQL DE replication tables"
echo "========================================"

docker exec "$DE_CONTAINER" \
    psql \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -c "
        SELECT
            nspname AS schema,
            relname AS table_name,
            set_name AS replication_set
        FROM spock.tables
        WHERE set_name IS NOT NULL
        ORDER BY nspname, relname, set_name;
    "

echo
echo "========================================"
echo " Spock configuration completed"
echo "========================================"
