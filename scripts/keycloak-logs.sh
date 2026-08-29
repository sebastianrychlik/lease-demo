#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

docker compose \
  -f "$ROOT_DIR/infrastructure/keycloak/docker-compose.yml" \
  logs -f keycloak
