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

# ---------------------------------------------------------------------------
# Persistent local crypto keys
# ---------------------------------------------------------------------------

# Application-level field encryption keys (PESEL) are required at startup.
# The local PostgreSQL database is persistent, so these keys must also remain
# stable across backend restarts.
#
# .env.local is generated once, reused on subsequent runs and never committed
# or printed.
#
# shellcheck source=lib/local-crypto-keys.sh
source "$ROOT_DIR/scripts/lib/local-crypto-keys.sh"
load_local_crypto_keys "$ROOT_DIR"

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------

cleanup() {
    echo
    echo "Stopping LeaseDemo local processes..."

    if [ -n "${BACKEND_PID:-}" ]; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi

    if [ -n "${FRONTEND_PID:-}" ]; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi

    echo "Angular and backend stopped."
    echo "Keycloak container remains running."
}

trap cleanup EXIT INT TERM

# ---------------------------------------------------------------------------
# 1. Keycloak
# ---------------------------------------------------------------------------

echo
echo "[1/4] Starting Keycloak..."

if docker ps --format '{{.Names}}' | grep -qx "lease-demo-keycloak"; then
    echo "Keycloak is already running."
else
    docker start lease-demo-keycloak
fi

# ---------------------------------------------------------------------------
# 2. Angular/Vite local cache
# ---------------------------------------------------------------------------

echo
echo "[2/4] Preparing Angular development cache..."

ANGULAR_CACHE_DIR="$ROOT_DIR/frontend/.angular/cache"

if [ -d "$ANGULAR_CACHE_DIR" ]; then
    echo "Removing stale Angular/Vite cache..."
    rm -rf "$ANGULAR_CACHE_DIR"
fi

echo "Angular/Vite cache ready."

# ---------------------------------------------------------------------------
# 3. Spring Boot
# ---------------------------------------------------------------------------

echo
echo "[3/4] Starting Spring Boot..."

(
    cd "$ROOT_DIR/backend"
    mvn "${MAVEN_PROFILE_ARGS[@]}" spring-boot:run
) &

BACKEND_PID=$!

# ---------------------------------------------------------------------------
# 4. Angular
# ---------------------------------------------------------------------------

echo
echo "[4/4] Starting Angular..."

(
    cd "$ROOT_DIR/frontend"
    npm start
) &

FRONTEND_PID=$!

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------

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

wait
