#!/usr/bin/env bash

set -euo pipefail

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
echo

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
    echo "Stopping LeaseDemo local application processes..."

    if [ -n "${BACKEND_PID:-}" ]; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi

    if [ -n "${FRONTEND_PID:-}" ]; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi

    echo "Angular and Spring Boot stopped."
    echo "Docker infrastructure remains running."
}

trap cleanup EXIT INT TERM

# ---------------------------------------------------------------------------
# 1. Docker infrastructure
# ---------------------------------------------------------------------------

echo
echo "[1/6] Starting Docker infrastructure..."

"$ROOT_DIR/scripts/infrastructure-start.sh"

# ---------------------------------------------------------------------------
# 2. Database migrations (by FlyWay)
# ---------------------------------------------------------------------------

echo
echo "[2/6] Migrating PostgreSQL PL + DE..."

"$ROOT_DIR/scripts/database-migrate.sh"

echo "Database schemas are up to date."

# ---------------------------------------------------------------------------
# 3. Spock replication configuration (list of tables to be replicated)
# ---------------------------------------------------------------------------

echo
echo "[3/6] Configuring Spock replication..."

"$ROOT_DIR/scripts/spock-configure.sh"

echo "Spock replication configuration is up to date."


# ---------------------------------------------------------------------------
# 4. Angular/Vite local cache
# ---------------------------------------------------------------------------

echo
echo "[4/6] Preparing Angular development cache..."

ANGULAR_CACHE_DIR="$ROOT_DIR/frontend/.angular/cache"

if [ -d "$ANGULAR_CACHE_DIR" ]; then
    echo "Removing stale Angular/Vite cache..."
    rm -rf "$ANGULAR_CACHE_DIR"
fi

echo "Angular/Vite cache ready."

# ---------------------------------------------------------------------------
# 5. Spring Boot
# ---------------------------------------------------------------------------

echo
echo "[5/6] Starting Spring Boot..."

(
    cd "$ROOT_DIR/backend"
    mvn "${MAVEN_PROFILE_ARGS[@]}" spring-boot:run
) &

BACKEND_PID=$!

# ---------------------------------------------------------------------------
# 5. Angular
# ---------------------------------------------------------------------------

echo
echo "[6/6] Starting Angular..."

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
echo "Angular:       http://localhost:4200"
echo "Backend:       http://localhost:8080"
echo "Swagger:       http://localhost:8080/swagger-ui/index.html"
echo "Keycloak:      http://localhost:8081"
echo "Kafka UI:      http://localhost:8090"
echo "Mailpit:       http://localhost:8025"
echo "Elasticsearch: http://localhost:9200"

if [ "${1:-}" = "--local-seed" ]; then
    echo
    echo "Local seed tooling: INCLUDED"
    echo "Maven profile:       local-seed"
else
    echo
    echo "Local seed tooling: NOT INCLUDED"
fi

echo
echo "Press Ctrl+C to stop Angular and Spring Boot."
echo "Docker infrastructure will remain running."

wait
