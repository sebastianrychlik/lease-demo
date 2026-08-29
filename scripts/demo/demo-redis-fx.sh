#!/usr/bin/env bash

set -e

REDIS_CONTAINER="lease-demo-redis-shared"

FX_KEY="fx:EUR:PLN"

echo "========================================"
echo " LeaseDemo — Redis FX Cache Demo"
echo "========================================"
echo

echo "[1/4] Removing cached EUR/PLN..."
docker exec "$REDIS_CONTAINER" \
    redis-cli DEL "$FX_KEY"

echo
echo "[2/4] First backend request."
echo "Expected:"
echo "  CACHE MISS"
echo "  NBP API called"
echo "  Value stored in Redis"
echo

curl -s \
    "http://localhost:8080/api/exchange-rates?base=EUR&currency=PLN"

echo
echo
echo "[3/4] Redis value:"
docker exec "$REDIS_CONTAINER" \
    redis-cli GET "$FX_KEY"

echo
echo
echo "TTL:"
docker exec "$REDIS_CONTAINER" \
    redis-cli TTL "$FX_KEY"

echo
echo
echo "[4/4] Second backend request."
echo "Expected:"
echo "  CACHE HIT"
echo "  NBP API NOT called"
echo

curl -s \
    "http://localhost:8080/api/exchange-rates?base=EUR&currency=PLN"

echo
