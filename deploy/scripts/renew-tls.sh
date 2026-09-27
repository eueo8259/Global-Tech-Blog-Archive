#!/usr/bin/env bash

set -euo pipefail

repo_root="${TECHPORT_REPO_ROOT:-/home/ubuntu/Global-Tech-Blog-Archive}"

certbot renew \
  --webroot \
  --webroot-path /var/www/certbot \
  --quiet \
  --deploy-hook "cd ${repo_root} && docker compose --env-file .env -f docker-compose.prod.yml exec -T nginx nginx -s reload"
