# TechPort Single EC2 Deployment

This setup runs the production backend stack on one EC2 instance with Docker Compose.

## Services

- `nginx`: public gateway on ports `80` and `443`
- `backend`: Spring Boot API on the internal Docker network
- `mysql`: MySQL 8.4 on the internal Docker network

Only expose ports `22`, `80`, and `443` in the EC2 security group. Do not open
`8080` or `3306` to the internet.

## HTTPS Certificates

Nginx terminates HTTPS for `api.techport.dev` and proxies API requests to the
backend container over the internal Docker network.

Certificate files live on the EC2 host under `/etc/letsencrypt` and are mounted
read-only into the Nginx container. Do not copy certificate files, private keys,
or Certbot account files into this repository.

Before deploying the HTTPS Nginx config for the first time, issue the
certificate on the EC2 host. The first issuance can use standalone mode while
Nginx is stopped because no certificate exists yet.

```bash
sudo mkdir -p /var/www/certbot
docker compose --env-file .env -f docker-compose.prod.yml stop nginx
sudo certbot certonly --standalone -d api.techport.dev
docker compose --env-file .env -f docker-compose.prod.yml up -d
```

After the HTTPS config is running, renew with the webroot path served by Nginx.

```bash
sudo certbot renew --webroot -w /var/www/certbot
docker compose --env-file .env -f docker-compose.prod.yml exec nginx nginx -s reload
```

Check HTTPS after deployment.

```bash
curl -i https://api.techport.dev/health
curl -i https://api.techport.dev/api/articles
curl -I http://api.techport.dev/health
```

## First Deploy

Create an `.env` file from the production example and fill real secret values.

```bash
cp .env.prod.example .env
vi .env
```

Start the stack after the backend image exists in ECR and the EC2 instance can
pull from ECR.

```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d
```

Check the gateway and API.

```bash
curl -i https://api.techport.dev/health
curl -i https://api.techport.dev/api/articles
```

## Operations

View containers.

```bash
docker compose --env-file .env -f docker-compose.prod.yml ps
```

View logs.

```bash
docker compose --env-file .env -f docker-compose.prod.yml logs -f
```

Stop the stack.

```bash
docker compose --env-file .env -f docker-compose.prod.yml down
```

Keep the `mysql-data` Docker volume unless you intentionally want to remove the
database.

## GitHub Actions CD

Backend CD deploys the single EC2 Docker Compose stack from the `main` branch.
The workflow builds a Linux ARM64 backend image, pushes it to ECR, sends an SSM
Run Command to the EC2 instance, pulls the exact image tag, restarts the stack,
and checks the Nginx health endpoint.

Required repository secrets:

| Secret                       | Description                                         |
| ---------------------------- | --------------------------------------------------- |
| `AWS_REGION`                 | AWS region, for example `ap-northeast-2`            |
| `BACKEND_AWS_ROLE_TO_ASSUME` | GitHub Actions deploy role ARN                      |
| `BACKEND_EC2_INSTANCE_ID`    | Target EC2 instance ID                              |
| `BACKEND_ECR_REPOSITORY`     | ECR repository name, for example `techport-backend` |

Required AWS setup:

- GitHub Actions OIDC role can push to and read from the backend ECR repository,
  and send SSM commands to the target EC2 instance.
  Docker push checks the pushed image manifest, so the role needs ECR read
  actions such as `ecr:BatchGetImage` in addition to write actions.
- EC2 instance profile has `AmazonSSMManagedInstanceCore` and
  `AmazonEC2ContainerRegistryReadOnly`.
- SSM Agent is running and the EC2 instance appears as a managed node.
- The EC2 instance has Docker, Docker Compose, Git, and AWS CLI. The workflow
  installs AWS CLI v2 on first deploy if it is missing.

The workflow intentionally keeps production application secrets in the EC2
`.env` file. It does not copy database passwords or API keys from GitHub
Secrets into the server.
