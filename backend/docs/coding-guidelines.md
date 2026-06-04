# Backend Coding Guidelines

## General Style

- Use Java 21 and Spring Boot idioms.
- Prefer clear, direct code over clever abstractions.
- Keep methods small enough to read without hiding simple behavior behind unnecessary helpers.
- Use names that describe domain behavior, not implementation mechanics.
- Avoid ternary expressions for meaningful control flow; prefer explicit `if` statements.

## MVP First

Prefer solving the current requirement cleanly.

Avoid:

- premature optimization
- speculative extension points
- generic frameworks for one known use case
- broad refactors near a feature change

## DTOs And Records

Use Java records for simple immutable request/response DTOs when behavior is not needed.

Do not expose JPA entities directly through API responses.

## Domain Objects

Prefer:

- encapsulated state changes
- meaningful factory methods when object creation is needed
- no public setters unless there is a concrete need

Avoid unrestricted mutation on entities.

## Dependencies

Do not add backend dependencies without discussion.

When a standard library, Spring Boot feature, or Spring Data JPA feature is enough, use it before introducing a dependency.

## Lombok

Lombok may be used to reduce simple boilerplate such as getters and constructor injection.

Prefer:

- `@Getter` for JPA entity getters
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for JPA entity default constructors
- `@RequiredArgsConstructor` for Spring constructor injection

Avoid:

- `@Setter` on entities
- `@Data` on entities
- Lombok annotations that hide meaningful domain behavior
