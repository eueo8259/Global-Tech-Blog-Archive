#!/usr/bin/env bash

set -euo pipefail

repo_root="${TECHPORT_REPO_ROOT:-/home/ubuntu/Global-Tech-Blog-Archive}"
docker_path="$(command -v docker)"

certbot renew \
  --webroot \
  --webroot-path /var/www/certbot \
  --quiet \
  --deploy-hook "${docker_path} compose --project-directory ${repo_root} --env-file ${repo_root}/.env -f ${repo_root}/docker-compose.prod.yml exec -T nginx nginx -s reload"
