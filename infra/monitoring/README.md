# TechPort Monitoring

별도 ARM64 EC2에서 Prometheus, Grafana, Nginx, Certbot을 Docker Compose로 실행한다.
Grafana와 Prometheus 포트는 외부에 공개하지 않고 Nginx의 80/443만 공개한다.

## 1. 모니터링 디렉터리만 받기

```bash
sudo mkdir -p /opt/techport-monitoring
sudo chown ubuntu:ubuntu /opt/techport-monitoring
git clone --filter=blob:none --sparse \
  https://github.com/eueo8259/Global-Tech-Blog-Archive.git \
  /opt/techport-monitoring
cd /opt/techport-monitoring
git sparse-checkout set infra/monitoring
git checkout develop
```

## 2. 환경 변수 설정

```bash
cd /opt/techport-monitoring/infra/monitoring
cp .env.example .env
nano .env
```

`CERTBOT_EMAIL`과 `GRAFANA_ADMIN_PASSWORD`는 실제 값으로 변경한다.
`.env`는 Git에 추가하지 않는다.

## 3. 최초 HTTPS 인증서 발급 및 실행

DNS의 `monitor.techport.dev` A 레코드가 모니터링 EC2를 가리켜야 한다.

```bash
bash scripts/bootstrap-tls.sh
docker compose --env-file .env -f docker-compose.monitoring.yml ps
```

브라우저에서 `https://monitor.techport.dev`로 접속한다.

## 4. 인증서 갱신

먼저 수동 갱신을 확인한다.

```bash
bash scripts/renew-tls.sh
```

그다음 `crontab -e`에 매일 실행할 작업을 추가한다. 실제 갱신은 인증서 만료가
가까울 때만 수행된다.

```cron
0 3 * * * cd /opt/techport-monitoring/infra/monitoring && bash scripts/renew-tls.sh >> /home/ubuntu/monitoring-certbot.log 2>&1
```

## 운영 명령

```bash
docker compose --env-file .env -f docker-compose.monitoring.yml ps
docker compose --env-file .env -f docker-compose.monitoring.yml logs -f
docker compose --env-file .env -f docker-compose.monitoring.yml pull
docker compose --env-file .env -f docker-compose.monitoring.yml up -d
```

Prometheus는 현재 자기 자신만 수집한다. 개발·운영 Spring Boot 메트릭 연결은
별도 이슈에서 추가한다.
