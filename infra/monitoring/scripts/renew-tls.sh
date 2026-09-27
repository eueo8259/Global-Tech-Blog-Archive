#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONITORING_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${MONITORING_DIR}/docker-compose.monitoring.yml"
ENV_FILE="${MONITORING_DIR}/.env"

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
  --profile tools run --rm certbot renew \
  --webroot --webroot-path /var/www/certbot --quiet
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
  exec nginx nginx -s reload
