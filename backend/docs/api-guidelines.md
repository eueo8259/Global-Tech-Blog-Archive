# Backend API Guidelines

## Controller Rules

Controllers should handle:

- HTTP request parameters
- simple validation of HTTP-facing values
- HTTP status mapping
- request/response DTO mapping

Controllers should not contain business rules or repository calls.

## Response Shape

Return DTOs, not JPA entities.

For list endpoints, prefer response objects that include both items and page metadata when pagination is part of the contract.

## Error Handling

Return `400 Bad Request` for invalid client-provided query parameters.

Keep validation messages concise and specific enough to diagnose the request issue.

Use `global/error` for common API error formatting:

- `ErrorCode` defines shared HTTP status, code, and default message values.
- `ErrorResponse` is the client-facing error body.
- `GlobalExceptionHandler` is the final API error response boundary.
- Domain-specific exceptions should live inside the relevant domain package when they become necessary.

Controllers should not wrap service calls in local `try/catch` only to map known errors to HTTP responses. Prefer throwing a business/input exception and handling it globally.

## Category Filters

Root `docs/domain-model.md` defines stored article categories. Backend APIs must preserve that boundary.

`ALL` is an API/UI filter option only. It must not be added to `ArticleCategory` or stored in the database.

When an API accepts `ALL`, handle that conversion outside the domain enum.

## Public Contract Changes

Discuss before changing an API contract that frontend code already depends on.
