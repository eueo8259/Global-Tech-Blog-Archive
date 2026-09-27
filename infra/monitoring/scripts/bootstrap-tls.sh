#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONITORING_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${MONITORING_DIR}/docker-compose.monitoring.yml"
ENV_FILE="${MONITORING_DIR}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Create ${ENV_FILE} from .env.example first."
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

: "${MONITORING_DOMAIN:?MONITORING_DOMAIN must be set}"
: "${CERTBOT_EMAIL:?CERTBOT_EMAIL must be set}"
: "${GRAFANA_ADMIN_PASSWORD:?GRAFANA_ADMIN_PASSWORD must be set}"

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" pull
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
  --profile bootstrap up -d prometheus grafana bootstrap-nginx

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
  --profile tools run --rm certbot certonly \
  --webroot --webroot-path /var/www/certbot \
  --domain "${MONITORING_DOMAIN}" \
  --email "${CERTBOT_EMAIL}" \
  --agree-tos --non-interactive --no-eff-email

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
  --profile bootstrap stop bootstrap-nginx
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
  --profile bootstrap rm -f bootstrap-nginx
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d
