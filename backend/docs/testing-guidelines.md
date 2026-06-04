# Backend Testing Guidelines

## Testing Principles

Tests should verify behavior, not implementation details.

Prefer:

* small and focused tests
* one behavior per test
* explicit test data
* readable assertions

Avoid:

* testing framework behavior
* testing trivial getters or setters
* asserting internal implementation details
* creating large object graphs when unnecessary

---

## Test Types

### Domain Tests

Use plain JUnit tests.

Test:

* state changes
* validation rules
* factory methods
* domain behavior

Do not involve Spring or the database.

---

### Service Tests

Use unit tests with Mockito.

Test:

* business rules
* branching logic
* exception scenarios
* interaction with collaborators when relevant

Mock:

* repositories
* external services
* infrastructure dependencies

Do not load the Spring context unless required.

---

### Repository Tests

Use integration tests with the real persistence layer.

Test:

* custom queries
* filtering
* sorting
* pagination
* entity mappings

Prefer testing against the same database engine used by the application. Do not mock JPA repositories.

---

### Controller Tests

Use MVC slice tests.

Test:

* request validation
* request parameter binding
* HTTP status codes
* response structure
* error responses

Mock service dependencies. Do not verify business logic through controller tests.

---

## Test Naming

Describe the expected behavior.

Good:

```java
getArticlesReturnsArticlesWithSourceInformation()

createArticleThrowsExceptionWhenSourceDoesNotExist()

findArticlesReturnsNextPageWhenCursorExists()
```

Avoid:

```java
testGetArticles()

articleServiceTest()

repositoryTest()
```

---

## Test Data

Keep test fixtures minimal.

Prefer:

* explicit setup
* factory methods
* test fixture builders when repeated setup becomes noisy

Avoid:

* large shared fixtures
* hidden test data
* unnecessary relationships

---

## When Tests Are Required

Add or update tests when changing:

* business rules
* validation logic
* repository queries
* API contracts
* transaction-sensitive behavior
* bug fixes

Bug fixes should include a regression test whenever practical.

---

## Verification

Run from `backend/` before reporting completion:

```bash
./gradlew test build
```

If repository tests require MySQL:

```bash
docker compose up -d mysql
```

If runtime behavior changes:

1. Run the application locally.
2. Verify the affected endpoint or flow manually.
3. Confirm logs and responses match expectations.
