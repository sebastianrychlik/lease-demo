#!/usr/bin/env bash

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "========================================"
echo " LeaseDemo — Local Environment"
echo "========================================"

echo
echo "[1/3] Starting Keycloak..."

if docker ps --format '{{.Names}}' | grep -qx "lease-demo-keycloak"; then
    echo "Keycloak is already running."
else
    docker start lease-demo-keycloak
fi

echo
echo "[2/3] Starting Spring Boot..."

(
    cd "$ROOT_DIR/backend"
    mvn spring-boot:run
) &

BACKEND_PID=$!

echo
echo "[3/3] Starting Angular..."

(
    cd "$ROOT_DIR/frontend"
    npm start
) &

FRONTEND_PID=$!

echo
echo "========================================"
echo " LeaseDemo is starting"
echo "========================================"
echo
echo "Angular:     http://localhost:4200"
echo "Backend:     http://localhost:8080"
echo "Keycloak:    http://localhost:8081"
echo
echo "Press Ctrl+C to stop Angular and backend."

cleanup() {
    echo
    echo "Stopping LeaseDemo local processes..."

    kill "$BACKEND_PID" 2>/dev/null || true
    kill "$FRONTEND_PID" 2>/dev/null || true

    echo "Angular and backend stopped."
    echo "Keycloak container remains running."
}

trap cleanup EXIT INT TERM

wait
