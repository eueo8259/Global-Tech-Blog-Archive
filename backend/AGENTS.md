# Backend AGENTS

Backend-specific instructions for Global Tech Blog Archive.

The root `AGENTS.md` contains repository-wide rules. This file narrows those rules for backend work.

Rule scope:

- root `AGENTS.md`: project-wide rules and common constraints
- root `docs/`: system architecture, domain model, source strategy, Git workflow, and documentation workflow
- `backend/AGENTS.md`: backend rules an AI agent must read before changing backend files
- `backend/docs/`: backend implementation, API, persistence, and testing details

## Always Follow

- Read the relevant backend docs before implementation.
- Apply the root MVP/simplicity principles to backend code.
- Prefer simple Spring Boot, Spring Data JPA, and Java 21 patterns already used in this project.
- Do not mix backend implementation work with unrelated formatting, IDE files, or frontend changes.
- Avoid adding abstractions for future use cases that are not part of the current requirement.

## Backend Docs

Read the docs that match the task before editing code:

```text
backend/docs/architecture.md            # Backend layer responsibilities and package direction
backend/docs/coding-guidelines.md       # Java/Spring implementation style
backend/docs/testing-guidelines.md      # Backend test style and verification
backend/docs/api-guidelines.md          # Controller, DTO, and API response rules
backend/docs/persistence-guidelines.md  # JPA, repositories, transactions, and DB rules
```

For domain model, source strategy, and repository-wide Git workflow, also follow the root docs:

```text
docs/domain-model.md
docs/article-source-strategy.md
docs/git-workflow.md
docs/documentation-workflow.md
```

## Work Order

Prefer implementing backend features in this order:

1. Confirm domain and persistence shape.
2. Add or adjust repository queries.
3. Add service/use-case logic.
4. Add API controller and DTOs.
5. Add focused tests.
6. Run verification.

## Testing Gate

When changing backend behavior, add or update tests. Follow `backend/docs/testing-guidelines.md` for detailed testing rules.

Minimum expectations:

* Domain behavior should be covered by unit tests.
* Service/use-case logic should be covered by focused tests.
* Repository queries should be covered by integration tests.
* API contract changes should be covered by controller or API tests.
* Bug fixes should include a regression test when practical.

Do not consider backend work complete if the relevant tests are missing or failing.

Before completing backend work, run:

```bash
./gradlew test build
```

## Safety Gates

Ask before backend changes that affect:

- database schema or Flyway migrations
- public API contracts already used by frontend
- transaction boundaries that affect existing behavior
- dependencies or Gradle configuration
- cross-cutting package structure

## Commands

Run from `backend/`:

```bash
./gradlew test
./gradlew build
./gradlew bootRun
```

Required backend verification before completion:

```bash
./gradlew test build
```

When the change affects runtime behavior, also run the application locally and manually check the relevant endpoint or flow.

## Common Mistakes To Avoid

- implementing directly on `main`
- placing business logic in controllers
- exposing JPA entities as API responses
- using `EAGER` relationships without discussion
- adding general-purpose abstractions for one known use case
- using ternary expressions for meaningful control flow
- leaving debug logs, unused code, or unrelated IDE changes in the work
