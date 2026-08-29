#!/usr/bin/env bash

set -e

KAFKA_CONTAINER="lease-demo-kafka-events"
CONNECT_CONTAINER="lease-demo-kafka-connect-cdc"

echo "========================================"
echo " LeaseDemo — Kafka Recovery Demo"
echo " Outbox + Debezium + Kafka"
echo "========================================"
echo

echo "[1/6] Stopping Kafka..."
docker stop "$KAFKA_CONTAINER"

echo
echo "Kafka is DOWN."
echo

echo "[2/6] Kafka Connect / Debezium remains configured."
docker ps \
    --filter "name=$CONNECT_CONTAINER" \
    --format "table {{.Names}}\t{{.Status}}"

echo
echo "[3/6] Creating lease applications while Kafka is DOWN."
echo
echo "TODO:"
echo "  Playwright will submit multiple lease applications here."
echo

# Later:
#
# cd frontend
# npx playwright test e2e/demo/kafka-backlog.spec.ts

read -r -p "Create several applications, then press ENTER..."

echo
echo "[4/6] Business records should exist in PostgreSQL."
echo "Outbox events should also exist."
echo
echo "Kafka is still DOWN."
echo "No asynchronous PDF/email processing should have completed yet."

read -r -p "Press ENTER to restore Kafka..."

echo
echo "[5/6] Starting Kafka..."
docker start "$KAFKA_CONTAINER"

echo
echo "Waiting for Kafka..."
sleep 8

echo
echo "[6/6] Kafka restored."
echo
echo "Expected flow:"
echo
echo " PostgreSQL WAL"
echo "       ↓"
echo " Debezium catch-up"
echo "       ↓"
echo " Kafka event backlog"
echo "       ↓"
echo " PDF generator"
echo "       ↓"
echo " DocumentGenerated"
echo "       ↓"
echo " Notification consumer"
echo "       ↓"
echo " Mailpit"
echo
echo "Open:"
echo "  Kafka UI: http://localhost:8090"
echo "  Mailpit:  http://localhost:8025"
