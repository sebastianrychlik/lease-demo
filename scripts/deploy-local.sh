#!/usr/bin/env bash

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# ---------------------------------------------------------------------------
# Arguments
# ---------------------------------------------------------------------------

MAVEN_PROFILE_ARGS=()

case "${1:-}" in
    "")
        ;;
    --local-seed)
        MAVEN_PROFILE_ARGS+=("-Plocal-seed")
        ;;
    *)
        echo "Unknown option: $1"
        echo
        echo "Usage:"
        echo "  ./scripts/deploy-local.sh"
        echo "  ./scripts/deploy-local.sh --local-seed"
        exit 1
        ;;
esac

echo "========================================"
echo " LeaseDemo — Local Environment"
echo "========================================"

if [ "${1:-}" = "--local-seed" ]; then
    echo "Mode: LOCAL + seed tooling"
else
    echo "Mode: LOCAL"
fi

echo
echo "[1/3] Starting Keycloak..."

if docker ps --format '{{.Names}}' | grep -qx "lease-demo-keycloak"; then
    echo "Keycloak is already running."
else
    docker start lease-demo-keycloak
fi

echo
echo "[2/3] Starting Spring Boot..."

# Application-level field encryption keys (PESEL) are required at startup.
# The local PostgreSQL database is persistent, so these keys must also be
# persistent across backend restarts (see scripts/lib/local-crypto-keys.sh
# and .env.local — never committed, never printed).
# shellcheck source=lib/local-crypto-keys.sh
source "$ROOT_DIR/scripts/lib/local-crypto-keys.sh"
load_local_crypto_keys "$ROOT_DIR"

(
    cd "$ROOT_DIR/backend"

    mvn "${MAVEN_PROFILE_ARGS[@]}" spring-boot:run
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
echo "Swagger:     http://localhost:8080/swagger-ui/index.html"
echo "Keycloak:    http://localhost:8081"

if [ "${1:-}" = "--local-seed" ]; then
    echo
    echo "Local seed tooling: INCLUDED"
    echo "Maven profile:       local-seed"
else
    echo
    echo "Local seed tooling: NOT INCLUDED"
fi

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
