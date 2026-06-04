# Global Tech Blog Archive

Engineering team blog archive service. Built for developers who want to discover technical articles, engineering stories, architecture decisions, and development experiences shared by engineering teams. Currently in MVP development.

## Tech Layers

- **Backend**: Java 21, Spring Boot 3.5, Spring Data JPA
- **Frontend**: React 19, TypeScript 5, Vite
- **Database**: MySQL 8.4 LTS

## Project Structure

```text
backend/      # Java 21, Spring Boot, Spring Data JPA
frontend/     # React, TypeScript, Vite
docs/         # Repository-wide design notes and decisions
```

## Scope-specific Instructions

Use the nearest `AGENTS.md` for detailed instructions. Rules become more specific as the directory scope gets narrower.
[
```text
AGENTS.md              # Repository-wide rules
backend/AGENTS.md      # Backend-specific rules
frontend/AGENTS.md     # Frontend-specific rules, if added later

```

When working in a subdirectory, follow this file first, then the nearest lower-scope `AGENTS.md`. Lower-scope files may add concrete implementation and testing rules for that area.

## Key Files

```text
backend/
|-- src/main/java/       # Backend application source code
|-- src/test/java/       # Backend tests
|-- src/main/resources/  # Backend configuration files
|-- docs/                # Backend-specific implementation guidance
`-- build.gradle         # Backend build and dependency configuration

frontend/
|-- src/                 # Frontend application source code
|-- package.json         # Frontend scripts and dependencies
`-- vite.config.ts       # Vite configuration

docs/
|-- git-workflow.md              # Branch, issue, commit, and PR workflow rules
|-- article-source-strategy.md   # Article source, company, and category strategy
|-- architecture.md              # MVP system architecture and responsibility boundaries
|-- domain-model.md              # MVP database schema and domain model
`-- documentation-workflow.md    # Repo docs and Notion documentation workflow
```

## Commands

Use these as project entrypoints. For scope-specific verification, follow the nearest `AGENTS.md`.

```bash
# Backend
cd backend
./gradlew test        # Unit tests
./gradlew build       # Build
./gradlew bootRun     # Development server (port: 8080)

# Frontend
cd frontend
npm install           # Install dependencies
npm run dev           # Development server (port: 5173)
npm run build         # Typecheck + production build
npm run lint          # Lint

# Database
docker compose up -d mysql    # Start MySQL development DB (host port: 3307)
docker compose down           # Stop MySQL development DB
```

- Backend typecheck: included in `./gradlew build`
- Frontend typecheck: included in `npm run build`
- E2E test: TBD
- Required checks before completion depend on the touched scope.

## Engineering Principles

### Development Approach

* Prefer MVP over completeness.
* Prefer simple solutions over complex abstractions.
* Build functionality incrementally in small steps.
* Authentication is not required for the initial MVP.

### Change Management

Discuss the following changes before implementation:

* Architecture changes
* Database schema changes
* New dependency additions

### Implementation Gate

Before code implementation or repo-tracked file edits for a feature/change:

* Check the current branch.
* Do not implement on `main`.
* If not already on an issue branch, stop and create or ask for the GitHub Issue and branch first.
* Working branches must follow docs/git-workflow.md.

### Documentation

- Repository-wide design decisions belong in root `docs/`.
- Scope-specific implementation guidance belongs under that scope, such as `backend/docs/`.
- Detailed Git workflow rules belong in `docs/git-workflow.md`.
- Article source and category strategy belongs in `docs/article-source-strategy.md`.
- System architecture and domain model decisions belong in `docs/architecture.md` and `docs/domain-model.md`.
- Backend implementation architecture, coding, API, persistence, and testing rules belong in `backend/docs/`.
- Notion documentation workflow belongs in `docs/documentation-workflow.md`.

## Common Mistakes to Avoid

- Ignoring a lower-scope `AGENTS.md` when working in a subdirectory.
- Duplicating narrow implementation rules in root-level docs.
- Mixing unrelated scope changes in one PR.

## Git Workflow

Follow docs/git-workflow.md.
