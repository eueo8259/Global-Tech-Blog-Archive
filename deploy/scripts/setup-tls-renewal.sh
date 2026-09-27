#!/usr/bin/env bash

set -euo pipefail

repo_root="${TECHPORT_REPO_ROOT:-/home/ubuntu/Global-Tech-Blog-Archive}"
service_name="techport-certbot-renew.service"
timer_name="techport-certbot-renew.timer"
verification_marker="/var/lib/techport/tls-renewal-dry-run-passed"

if ! command -v certbot >/dev/null 2>&1; then
  echo "certbot is required but not installed" >&2
  exit 1
fi

install -d -m 0755 /var/www/certbot /var/lib/techport
install -m 0755 "${repo_root}/deploy/scripts/renew-tls.sh" /usr/local/sbin/techport-renew-tls
install -m 0644 "${repo_root}/deploy/systemd/${service_name}" "/etc/systemd/system/${service_name}"
install -m 0644 "${repo_root}/deploy/systemd/${timer_name}" "/etc/systemd/system/${timer_name}"

systemctl daemon-reload
systemctl enable --now "${timer_name}"

TECHPORT_REPO_ROOT="${repo_root}" /usr/local/sbin/techport-renew-tls

if [[ ! -f "${verification_marker}" ]]; then
  certbot renew \
    --webroot \
    --webroot-path /var/www/certbot \
    --dry-run
  touch "${verification_marker}"
fi

systemctl is-enabled "${timer_name}"
systemctl is-active "${timer_name}"
systemctl list-timers "${timer_name}" --no-pager
