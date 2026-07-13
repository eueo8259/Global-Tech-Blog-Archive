# Backend Coding Guidelines

## General Style

- Use Java 21 and Spring Boot idioms.
- Prefer clear, direct code over clever abstractions.
- Keep methods small enough to read without hiding simple behavior behind unnecessary helpers.
- Use names that describe domain behavior, not implementation mechanics.
- Do not use ternary expressions in production Java code. Use explicit `if` statements so branches remain visible and extensible.
- Write code comments in Korean. Keep technical identifiers and standard terms unchanged when translating them would reduce clarity.

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

## Exception Handling

Use the shared `ErrorCode` and `BusinessException` hierarchy for backend errors
that can cross service or API boundaries.

`ErrorCode` owns the client-facing HTTP status, code, and default message:

- `status`: HTTP status returned by `GlobalExceptionHandler`
- `code`: stable client-facing error code
- `message`: default client-facing error message

Business exceptions should expose only these constructors:

```java
public SomeBusinessException(ErrorCode errorCode) {
    super(errorCode);
}

public SomeBusinessException(ErrorCode errorCode, String message) {
    super(errorCode, message);
}
```

Avoid:

- hard-coding a specific `ErrorCode` inside a custom exception class
- adding `Throwable cause` constructors to `BusinessException` subclasses
- storing transport details such as HTTP status codes in exception fields
- returning ad hoc error messages when an existing `ErrorCode` message is enough

Throw business exceptions by passing the `ErrorCode` at the call site:

```java
throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "size must be greater than 0");
```

Use the one-argument constructor when the default `ErrorCode` message is enough:

```java
throw new SlackTokenDecryptionException(ErrorCode.SLACK_TOKEN_DECRYPTION_ERROR);
```

Use plain Java exceptions such as `IllegalArgumentException` or
`IllegalStateException` only for internal invariants, programming errors, or
non-API flows that are not part of the shared error response contract.

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
