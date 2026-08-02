# Global Tech Blog Archive

Engineering team blog archive service for discovering technical articles,
engineering stories, architecture decisions, and development experiences from
engineering teams. Currently in MVP development.

## Project

- Backend: Java 21, Spring Boot 3.5, Spring Data JPA
- Frontend: React 19, TypeScript 5, Vite
- Database: MySQL 8.4 LTS

## Scope Rules

Read this file first, then read the nearest lower-scope `AGENTS.md` before
changing files in a subdirectory.

- `backend/AGENTS.md`: backend-specific rules
- `frontend/AGENTS.md`: frontend-specific rules
- `docs/git-workflow.md`: branch, issue, commit, and PR rules
- `backend/docs/`: backend implementation, API, persistence, and testing rules

## Core Principles

- Prefer MVP over completeness.
- Prefer simple solutions over complex abstractions.
- Build functionality incrementally in small steps.
- Match existing architecture and style before inventing new patterns.
- Keep changes surgical; do not mix unrelated scopes in one PR.
- Authentication is not required for the initial MVP.

## Safety Gates

Discuss before implementing changes that affect:

- architecture or cross-cutting package structure
- database schema or migrations
- dependencies or major version upgrades
- public API contracts or deployment settings

## Verification

Run the smallest relevant check that proves the change.

- Backend details: `backend/AGENTS.md` and `backend/docs/testing-guidelines.md`
- Frontend details: `frontend/AGENTS.md`, if present
- Report commands run and whether they passed
- If verification cannot run, explain why and what risk remains

## Git And PR Workflow

Follow `docs/git-workflow.md`.

- Do not implement on `main`.
- Work from an issue branch created from `develop`.
- Before branch, commit, push, or PR actions, run the automation checklist in `docs/git-workflow.md`.
- Use Conventional Commit style for commit messages and PR titles.
- Use `.github/PULL_REQUEST_TEMPLATE.md` for PR bodies when it exists.

## Documentation Map

- Repository design decisions: `docs/`
- Git workflow details: `docs/git-workflow.md`
- Code review workflow: `docs/code-review-workflow.md`
- Domain model: `docs/domain-model.md`
- Source strategy: `docs/article-source-strategy.md`
- Architecture: `docs/architecture.md`
- Notion workflow: `docs/documentation-workflow.md`
- Backend implementation guidance: `backend/docs/`
- Frontend implementation guidance: `frontend/docs/`
