# Frontend API Guidelines

This document defines how frontend code communicates with backend APIs.

For module boundaries and dependency direction, follow:

```text
frontend/docs/architecture.md
```

For backend-owned contracts and response behavior, follow:

```text
backend/docs/api-guidelines.md
backend/src/main/java/
docs/domain-model.md
docs/article-source-strategy.md
```

## Goal

Keep API communication predictable, type-safe, easy to modify, and separated from UI rendering.

Frontend code consumes backend APIs. It must not redefine backend contracts or duplicate backend business rules.

## Contract Source

Verify API behavior from backend docs, DTOs, controllers, or a running server before implementing frontend API code.

The backend owns:

- business rules
- validation rules
- response structures
- pagination rules
- filtering behavior

The frontend owns:

- request parameters derived from user interaction
- rendering returned data
- loading, empty, error, and success states
- navigation after user actions

## Response Types

Define TypeScript types for API responses.

```typescript
export interface ArticleSummary {
  id: number;
  title: string;
  sourceCompanyName: string;
}
```

Do not use `any` for API responses, and do not consume `response.json()` without assigning the parsed data to an explicit response type.

Do not invent fields that are not verified in the backend contract.

## API Module Location

Backend communication belongs in API modules, not page bodies or deeply nested UI components.

Preferred shape:

```text
features/article/api/articleApi.ts
```

API-backed screens should follow this flow:

```text
Page
  -> Feature hook
  -> Feature API module
  -> Shared API client
  -> Backend API
```

Feature API modules should build on a shared API client once common request behavior exists.

## Shared API Client

Use `shared/api/client.ts` for common behavior such as:

- base URL configuration
- common headers
- request configuration
- shared error mapping

Do not create a shared client before there is actual common behavior to centralize.

## Filtering And Pagination

The backend owns filtering and pagination semantics.

The frontend should collect user input, send request parameters that match the backend contract, and render the returned result.

Do not reimplement backend filtering or pagination rules locally unless the requirement explicitly calls for client-side behavior.

## API States

Every API-backed screen should handle:

- loading state
- empty state
- error state
- success state

Empty state is not an error state. API failures should produce visible user feedback instead of being silently ignored.

## Runtime Verification

When API-related behavior changes:

1. Run the frontend locally.
2. Verify the request path and parameters.
3. Verify loading, empty, error, and success states.

Do not verify only the success scenario.

## Common Mistakes To Avoid

- inventing response fields
- using `any` for API responses
- calling APIs directly from deeply nested UI components
- duplicating backend business rules
- assuming API behavior without checking backend docs, code, or runtime behavior
- hardcoding values that should come from the backend
