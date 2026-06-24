# TechPort Single EC2 Deployment

This setup runs the production backend stack on one EC2 instance with Docker Compose.

## Services

- `nginx`: public gateway on port `80`
- `backend`: Spring Boot API on the internal Docker network
- `mysql`: MySQL 8.4 on the internal Docker network

Only expose ports `22`, `80`, and later `443` in the EC2 security group. Do not
open `8080` or `3306` to the internet.

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
curl http://localhost/health
curl http://localhost/api/articles
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

- GitHub Actions OIDC role can push to the backend ECR repository and send SSM
  commands to the target EC2 instance.
- EC2 instance profile has `AmazonSSMManagedInstanceCore` and
  `AmazonEC2ContainerRegistryReadOnly`.
- SSM Agent is running and the EC2 instance appears as a managed node.
- The EC2 instance has Docker, Docker Compose, Git, and AWS CLI. The workflow
  installs AWS CLI v2 on first deploy if it is missing.

The workflow intentionally keeps production application secrets in the EC2
`.env` file. It does not copy database passwords or API keys from GitHub
Secrets into the server.
