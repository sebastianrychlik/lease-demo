#!/usr/bin/env bash

set -e

BACKEND_CONTAINER="lease-demo-backend-pl"
REDIS_CONTAINER="lease-demo-redis-shared"

echo "========================================"
echo " LeaseDemo — Redis Demo"
echo " Shared Draft / Stateless Backend"
echo "========================================"
echo

echo "[1/5] Checking Redis..."
docker exec "$REDIS_CONTAINER" redis-cli PING

echo
echo "[2/5] Current LeaseDemo draft keys:"
docker exec "$REDIS_CONTAINER" \
    redis-cli --scan --pattern 'lease:draft:*'

echo
echo "Now:"
echo "  1. Open Browser A"
echo "  2. Login as CUSTOMER"
echo "  3. Fill Step 1 and Step 2 of Lease Application"
echo
read -r -p "Press ENTER when draft is saved..."

echo
echo "[3/5] Redis draft keys:"
docker exec "$REDIS_CONTAINER" \
    redis-cli --scan --pattern 'lease:draft:*'

echo
echo "[4/5] Killing backend container..."
docker stop "$BACKEND_CONTAINER"

echo
echo "Backend is DEAD."
echo "Redis is still alive:"
docker exec "$REDIS_CONTAINER" redis-cli PING

echo
read -r -p "Press ENTER to restart backend..."

docker start "$BACKEND_CONTAINER"

echo
echo "[5/5] Backend restarted."
echo
echo "Open Browser B / Incognito and login as the same CUSTOMER."
echo
echo "Expected result:"
echo "  Draft is restored from Redis."
echo "  User continues from the previously saved step."
