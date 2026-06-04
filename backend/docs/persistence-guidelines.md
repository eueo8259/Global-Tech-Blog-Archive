# Backend Persistence Guidelines

## JPA Entities

Root `docs/domain-model.md` defines the database fields, constraints, and business rules. This document defines how backend JPA code should implement those decisions.

Prefer:

- `LAZY` relationships by default
- explicit column constraints matching Flyway migrations
- encapsulated entity state
- factory methods when tests or services need valid entity creation

Avoid:

- public setters without a concrete need
- `EAGER` relationships without discussion
- exposing entities through API responses

## Transactions

Use `@Transactional` at the service layer.

Use `@Transactional(readOnly = true)` for read-only service methods.

Avoid transaction management in controllers.

## Repository Queries

Prefer query options in this order:

1. Spring Data JPA derived queries
2. JPQL
3. QueryDSL

Use the simplest solution that satisfies the requirement. Do not introduce QueryDSL for trivial queries.

## Fetch Strategy

When a query needs related data for response mapping, prefer a targeted fetch strategy such as `@EntityGraph` on that repository method.

Do not change entity relationships to `EAGER` to solve one endpoint's loading need.

## Database Changes

Before changing schema:

1. Verify the change against root `docs/domain-model.md`.
2. Discuss breaking changes.
3. Confirm impact on existing data.

Use Flyway migrations for schema changes. Keep JPA annotations and migrations aligned.
