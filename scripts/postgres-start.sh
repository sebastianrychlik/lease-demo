#!/usr/bin/env bash
set -e

CONTAINER_NAME="lease-demo-postgres"
VOLUME_NAME="lease-demo-postgres-data"

DB_NAME="lease_demo"
DB_USER="leasedemo"
DB_PASSWORD="leasedemo"

echo "Starting LeaseDemo PostgreSQL..."

# Create persistent volume if it does not exist.
if ! docker volume inspect "$VOLUME_NAME" >/dev/null 2>&1; then
  echo "Creating Docker volume: $VOLUME_NAME"
  docker volume create "$VOLUME_NAME"
fi

# Create container if it does not exist.
if ! docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  echo "Creating PostgreSQL container..."

  docker run -d \
    --name "$CONTAINER_NAME" \
    --restart unless-stopped \
    -e POSTGRES_DB="$DB_NAME" \
    -e POSTGRES_USER="$DB_USER" \
    -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -p 5433:5432 \
    -v "$VOLUME_NAME:/var/lib/postgresql/data" \
    postgres:17

  echo "PostgreSQL container created."
else
  # Start existing container if stopped.
  if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME")" != "true" ]; then
    echo "Starting existing PostgreSQL container..."
    docker start "$CONTAINER_NAME"
  else
    echo "PostgreSQL is already running."
  fi
fi

echo
echo "PostgreSQL:"
echo "  Host:     localhost"
echo "  Port:     5433"
echo "  Database: $DB_NAME"
echo "  User:     $DB_USER"
echo
echo "Persistent volume: $VOLUME_NAME"
