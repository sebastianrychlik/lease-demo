#!/usr/bin/env bash

set -euo pipefail

PL_CONTAINER="lease-demo-postgres-pl"
DE_CONTAINER="lease-demo-postgres-de"

DB_NAME="lease_demo"
DB_USER="leasedemo"

TEST_VALUE="SPOCK-DEMO-$(date +%s)"
DE_VALUE="${TEST_VALUE}-FROM-DE"

echo "========================================"
echo " LeaseDemo — Spock Replication Demo"
echo " PL <-> DE"
echo "========================================"
echo

# ---------------------------------------------------------------------------
# PL -> DE
# ---------------------------------------------------------------------------

echo "[1/4] Writing test record to PostgreSQL PL..."

docker exec "$PL_CONTAINER" \
    psql \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -c "
        INSERT INTO replication_demo (
            id,
            source,
            message
        )
        VALUES (
            gen_random_uuid(),
            'PL',
            '$TEST_VALUE'
        );
    "

echo
echo "Waiting for replication..."
sleep 2

echo
echo "[2/4] Looking for PL record in PostgreSQL DE..."

PL_TO_DE_COUNT="$(
    docker exec "$DE_CONTAINER" \
        psql \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -tAc "
            SELECT COUNT(*)
            FROM replication_demo
            WHERE message = '$TEST_VALUE';
        "
)"

if [ "$PL_TO_DE_COUNT" != "1" ]; then
    echo "ERROR: PL -> DE replication failed."
    exit 1
fi

docker exec "$DE_CONTAINER" \
    psql \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -c "
        SELECT
            id,
            source,
            message,
            created_at
        FROM replication_demo
        WHERE message = '$TEST_VALUE';
    "

echo
echo "PL -> DE replication: OK"

# ---------------------------------------------------------------------------
# DE -> PL
# ---------------------------------------------------------------------------

echo
echo "[3/4] Writing test record to PostgreSQL DE..."

docker exec "$DE_CONTAINER" \
    psql \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -c "
        INSERT INTO replication_demo (
            id,
            source,
            message
        )
        VALUES (
            gen_random_uuid(),
            'DE',
            '$DE_VALUE'
        );
    "

echo
echo "Waiting for replication..."
sleep 2

echo
echo "[4/4] Looking for DE record in PostgreSQL PL..."

DE_TO_PL_COUNT="$(
    docker exec "$PL_CONTAINER" \
        psql \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -tAc "
            SELECT COUNT(*)
            FROM replication_demo
            WHERE message = '$DE_VALUE';
        "
)"

if [ "$DE_TO_PL_COUNT" != "1" ]; then
    echo "ERROR: DE -> PL replication failed."
    exit 1
fi

docker exec "$PL_CONTAINER" \
    psql \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -c "
        SELECT
            id,
            source,
            message,
            created_at
        FROM replication_demo
        WHERE message = '$DE_VALUE';
    "

echo
echo "DE -> PL replication: OK"

echo
echo "========================================"
echo " Spock bidirectional replication: PASS"
echo "========================================"
