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

Start the stack.

```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
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
