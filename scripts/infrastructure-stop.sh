#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infrastructure/docker-compose.yml"

echo "========================================"
echo " LeaseDemo — Infrastructure STOP"
echo "========================================"
echo

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "ERROR: Missing:"
    echo "  $COMPOSE_FILE"
    exit 1
fi

echo "Stopping Docker infrastructure..."
echo

docker compose \
    -f "$COMPOSE_FILE" \
    stop

echo
echo "Infrastructure stopped."
echo
echo "Persistent volumes were NOT removed."
echo "Database, Kafka, Redis, Elasticsearch and Mailpit data remain intact."
echo

echo "Container status:"
docker compose \
    -f "$COMPOSE_FILE" \
    ps -a
