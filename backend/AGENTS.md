# Backend AGENTS

Backend-specific instructions for Global Tech Blog Archive.

Read the root `AGENTS.md` first. This file narrows those rules for backend
work and points to the detailed backend docs.

## Scope

- Backend source: `backend/src/main/java/`
- Backend tests: `backend/src/test/java/`
- Backend config/resources: `backend/src/main/resources/`
- Backend implementation docs: `backend/docs/`

Do not mix backend work with unrelated frontend, IDE, formatting, or repository
metadata changes.

## Required Reading

Before backend changes, read the docs that match the task:

- `backend/docs/architecture.md`: layer responsibilities and package direction
- `backend/docs/coding-guidelines.md`: Java/Spring implementation style
- `backend/docs/testing-guidelines.md`: backend test style and verification
- `backend/docs/api-guidelines.md`: controller, DTO, and API response rules
- `backend/docs/persistence-guidelines.md`: JPA, repositories, transactions, DB rules

Also read repository-wide docs when relevant:

- `docs/domain-model.md`
- `docs/article-source-strategy.md`
- `docs/git-workflow.md`
- `docs/documentation-workflow.md`

## Backend Defaults

- Prefer simple Spring Boot, Spring Data JPA, and Java 21 patterns already used here.
- Keep feature-oriented packages and clear layer boundaries.
- Avoid abstractions for future use cases; add them only when they reduce real current complexity.
- Keep business logic out of controllers.
- Return DTOs from APIs, not JPA entities.

## Safety Gates

Ask before backend changes that affect:

- database schema, Flyway migrations, or existing data
- public API contracts already used by frontend code
- transaction boundaries that affect existing behavior
- Gradle dependencies, plugins, or major version upgrades
- cross-cutting package structure

## Verification

Follow `backend/docs/testing-guidelines.md`.

- Add or update focused tests when backend behavior changes.
- Run the smallest relevant backend check while working.
- Before completion, run `./gradlew test build` from `backend/`.
- If runtime behavior changed, run the app and manually verify the affected flow.
- Report commands run, pass/fail status, and any remaining risk.

## Commands

Run from `backend/`:

```bash
./gradlew test
./gradlew build
./gradlew test build
./gradlew bootRun
```
