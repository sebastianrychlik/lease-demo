#!/usr/bin/env bash
set -e

CONTAINER_NAME="lease-demo-postgres"

echo "Stopping LeaseDemo PostgreSQL..."

if ! docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  echo "PostgreSQL container does not exist."
  exit 0
fi

if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME")" = "true" ]; then
  docker stop "$CONTAINER_NAME"
  echo "PostgreSQL stopped."
else
  echo "PostgreSQL is already stopped."
fi

echo "Persistent database data has NOT been removed."
