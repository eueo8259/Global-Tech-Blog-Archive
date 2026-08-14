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
`TechPort/TechPort 인프라 현황` 대시보드는 파일 프로비저닝으로 자동 생성된다.
상단 서버 필터에서 `monitoring`, `dev`, `prod`를 선택할 수 있다.

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

## 수집 대상

Prometheus는 15초마다 애플리케이션과 EC2 호스트 메트릭을 수집한다.

```text
애플리케이션
개발  172.31.52.9:9090/actuator/prometheus
운영  172.31.15.143:9090/actuator/prometheus

EC2 호스트(Node Exporter)
모니터링  node-exporter:9100
개발      172.31.52.9:9100
운영      172.31.15.143:9100
```

두 애플리케이션 EC2의 보안그룹에는 TCP `9090`, `9100` 인바운드를 추가하되,
소스는 `techport-monitoring-sg`로 제한한다. 두 포트 모두 인터넷 전체에 공개하지 않는다.
