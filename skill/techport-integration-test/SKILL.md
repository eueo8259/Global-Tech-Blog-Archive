---
name: techport-integration-test
description: Run and diagnose TechPort end-to-end integration from Docker Compose MySQL through the Spring Boot API and Vite proxy to the React browser UI. Use when verifying frontend-backend integration, reproducing article loading failures, checking category filters with the real API, or preparing a local environment for browser review.
---

# TechPort Integration Test

Verify the real local stack in dependency order. Distinguish an empty dataset from an infrastructure failure, and leave processes that existed before the test untouched.

## Ground Rules

- Work from the repository root.
- Read the root `AGENTS.md` and the nearest scoped `AGENTS.md` before changing files.
- Do not edit code merely to make the test pass unless the user requests a fix.
- Record which containers and processes were already running.
- Stop only processes started during this test. Do not stop pre-existing Java, Node, MySQL, or Docker services.
- Use the in-app Browser for final UI verification. Make it visible when the user asks to inspect the screen.
- Treat `200` with `articles: []` as a successful integration with no article data.
- Treat timeout, connection refusal, proxy failure, or `5xx` as an integration failure.
- Do not call `POST /api/admin/article-crawls/run` without user approval. It performs network work and changes local data.

## Expected Local Topology

| Component | Address | Source |
| --- | --- | --- |
| MySQL | `127.0.0.1:3307` | `docker-compose.yml` maps container `3306` |
| Backend | `http://127.0.0.1:8080` | Spring Boot |
| Frontend | `http://127.0.0.1:5173` | Vite |
| Vite API proxy | `/api` to `http://localhost:8080` | `frontend/vite.config.ts` |

The local seed file creates companies and blog sources but may contain no articles. An empty article screen can therefore be correct.

## Workflow

### 1. Inspect Before Starting

Run:

```powershell
git status --short --branch
docker compose ps
netstat -ano | Select-String ':3307|:8080|:5173'
```

Note existing PIDs and container state. If a required port is occupied, identify whether the existing service belongs to this repository before reusing it.

### 2. Start And Verify MySQL

Start the project database:

```powershell
docker compose up -d mysql
docker compose ps
docker inspect global-tech-blog-archive-mysql --format '{{json .State.Health.Status}}'
```

Wait until health is `healthy`. If it does not become healthy, inspect:

```powershell
docker compose logs mysql
```

Do not continue to backend verification while `127.0.0.1:3307` is unavailable.

### 3. Start And Verify The Backend

If port `8080` is not already served by the correct project, start Spring Boot from `backend/` with `./gradlew.bat bootRun`. Use a hidden background process when interactive output is unnecessary, and retain its PID for cleanup.

Verify the backend directly before involving Vite:

```powershell
Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/articles?category=ALL&page=0&size=20' -UseBasicParsing -TimeoutSec 30
```

Require HTTP `200` and valid JSON. Both a populated `articles` array and an empty array are valid. On failure, inspect backend output and check these in order:

1. MySQL container health.
2. Port `3307` reachability.
3. `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` overrides.
4. Schema or seed initialization errors.
5. Backend stack trace.

### 4. Start And Verify The Frontend Proxy

If port `5173` is not already served by this project, start from `frontend/`:

```powershell
npm.cmd run dev -- --host 127.0.0.1
```

Verify the same API through Vite:

```powershell
Invoke-WebRequest -Uri 'http://127.0.0.1:5173/api/articles?category=ALL&page=0&size=20' -UseBasicParsing -TimeoutSec 30
```

The direct backend and proxied responses must have the same status and compatible JSON. A direct `200` with a proxied failure indicates Vite proxy configuration or frontend server trouble.

### 5. Verify In The Browser

Open `http://127.0.0.1:5173/` in the in-app Browser and check:

1. The TechPort header and category controls render.
2. `ALL` is selected initially.
3. Loading resolves to article cards or the empty-state message.
4. No error alert appears when the API returns `200`.
5. Selecting `BACKEND` changes its pressed state and sends a request containing `category=BACKEND`.
6. The result resolves to matching cards or a valid empty state.

When cards exist, verify title, company, category, date, and new-tab source link. Do not invent mock cards during this real integration check.

### 6. Run Automated Checks When Relevant

For code changes affecting the integrated path, run the existing project checks:

```powershell
Set-Location backend
.\gradlew.bat test build

Set-Location ..\frontend
npm.cmd run verify
```

The current Playwright suite uses mocked API responses. Report it as frontend behavior coverage, not proof that Docker, MySQL, and Spring Boot work together. The direct and proxied API checks provide that proof.

### 7. Report And Clean Up

Report each layer separately:

- MySQL container and health
- Backend direct API status and article count
- Vite proxied API status and article count
- Browser result for `ALL` and one selected category
- Automated check results, if run
- Remaining failure with the exact failing layer

Stop only background processes started during this run. Leave the MySQL container running when the user is continuing development; otherwise stop only the service started for this test with `docker compose stop mysql`. Never delete volumes unless explicitly requested.

## Failure Interpretation

| Observation | Meaning |
| --- | --- |
| `3307` closed | Project Docker MySQL is not available |
| Direct backend API times out then returns `500` | Usually DB connection or backend runtime failure; inspect logs |
| Direct API `200`, proxy API fails | Vite proxy or frontend dev server failure |
| Both APIs `200`, browser error alert | Frontend response handling failure |
| Both APIs `200`, browser empty state | Integration works; article data is absent for that filter |
| Mocked Playwright passes, real API fails | Frontend behavior works in isolation; full integration still fails |
