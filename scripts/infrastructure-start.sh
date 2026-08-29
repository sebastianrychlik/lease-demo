#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infrastructure/docker-compose.yml"

echo "========================================"
echo " LeaseDemo — Infrastructure START"
echo "========================================"
echo

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "ERROR: Missing:"
    echo "  $COMPOSE_FILE"
    exit 1
fi

# ---------------------------------------------------------------------------
# Kafka persistent storage (with WRITE permissions)
# ---------------------------------------------------------------------------

echo "Preparing Kafka persistent storage..."
echo

docker volume create lease-demo-kafka-data >/dev/null

docker run --rm \
    -u 0 \
    -v lease-demo-kafka-data:/data \
    alpine:3.22 \
    sh -c "chown -R 1000:1000 /data && chmod -R u+rwX /data"

echo "Kafka persistent storage ready."
echo

# ---------------------------------------------------------------------------
# Docker Compose
# ---------------------------------------------------------------------------

echo "Starting Docker infrastructure..."
echo

docker compose \
    -f "$COMPOSE_FILE" \
    up -d

echo
echo "Infrastructure started."
echo

echo "Containers:"
docker compose \
    -f "$COMPOSE_FILE" \
    ps

echo
echo "========================================"
echo " LeaseDemo — Infrastructure endpoints"
echo "========================================"
echo
echo "PostgreSQL PL:      localhost:5433"
echo "PostgreSQL DE:      localhost:5434"
echo "Redis:              localhost:6379"
echo "Kafka:              localhost:9092"
echo "Kafka Connect:      localhost:8083"
echo "Elasticsearch:      localhost:9200"
echo "Keycloak:           http://localhost:8081"
echo "Mailpit UI:         http://localhost:8025"
echo "Mailpit SMTP:       localhost:1025"
echo "Kafka UI:           http://localhost:8090"
echo
