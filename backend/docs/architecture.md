# Backend Architecture

## Overview

Root `docs/architecture.md` defines system components and ownership. This document narrows that architecture into backend package and layer rules.

The backend favors feature-oriented packages with clear layer responsibilities.

## Layer Responsibilities

### Controller

Responsibilities:

- HTTP request validation
- request parameter parsing
- request/response mapping
- HTTP status handling

Avoid:

- business logic
- repository access
- transaction orchestration
- exposing JPA entities directly

### Service

Responsibilities:

- business rules
- use-case orchestration
- transaction boundaries
- calling repositories

Avoid:

- HTTP-specific concerns
- presentation-only formatting
- direct coupling to frontend UI behavior

### Repository

Responsibilities:

- persistence access
- query implementation
- database-facing read/write operations

Avoid:

- business rules
- cross-domain orchestration
- API response shaping

## Package Direction

Prefer feature-oriented organization:

```text
article/
|-- api/
|-- application/
|-- domain/
`-- repository/

source/
|-- domain/
`-- repository/
```

Add deeper packages only when they clarify ownership. Avoid deep hierarchies without clear value.

## Development Shape

Implement backend features incrementally:

1. Domain model
2. Persistence
3. Service logic
4. API exposure
5. Tests

Do not introduce broad architecture changes together with feature implementation unless explicitly requested.
