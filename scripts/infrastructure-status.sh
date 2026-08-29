#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infrastructure/docker-compose.yml"

echo "========================================"
echo " LeaseDemo — Infrastructure STATUS"
echo "========================================"
echo

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "ERROR: Missing:"
    echo "  $COMPOSE_FILE"
    exit 1
fi

echo "Docker Compose services:"
echo

docker compose \
    -f "$COMPOSE_FILE" \
    ps

echo
echo "----------------------------------------"
echo " Container status / health"
echo "----------------------------------------"

CONTAINERS=(
    "lease-demo-keycloak"
    "lease-demo-postgres-pl"
    "lease-demo-postgres-de"
    "lease-demo-redis-shared"
    "lease-demo-kafka-events"
    "lease-demo-kafka-connect-cdc"
    "lease-demo-elasticsearch-eu-search"
    "lease-demo-mailpit"
    "lease-demo-kafka-ui"
)

for container in "${CONTAINERS[@]}"; do

    if ! docker inspect "$container" >/dev/null 2>&1; then
        printf "%-42s %-12s %s\n" "$container" "NOT CREATED" "-"
        continue
    fi

    STATUS="$(docker inspect \
        -f '{{.State.Status}}' \
        "$container")"

    HEALTH="$(docker inspect \
        -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}' \
        "$container")"

    printf "%-42s %-12s %s\n" \
        "$container" \
        "$STATUS" \
        "$HEALTH"
done
