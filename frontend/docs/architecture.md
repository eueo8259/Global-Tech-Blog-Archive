# Frontend Architecture

This document defines the frontend structure and responsibility boundaries for Global Tech Blog Archive.

For project-wide architecture, domain rules, source strategy, Git workflow, and agent working rules, follow:

```text
AGENTS.md
docs/architecture.md
docs/domain-model.md
docs/article-source-strategy.md
docs/git-workflow.md
frontend/AGENTS.md
```

## Goal

This document focuses on frontend code ownership and module boundaries. It does not repeat project tech stack, general MVP, API, dependency, verification, or safety-gate rules already defined in the root and frontend AGENTS files.

## Source Structure

Use a small feature-oriented structure when the current flat `src/` layout starts to grow:

```text
src/
|-- app/       # App, router, and application-level setup
|-- pages/     # Route-level screens composed from features
|-- features/  # Feature-specific UI, API, hooks, and types
|-- shared/    # Code reused across multiple features
`-- assets/    # Static assets
```

Do not introduce this structure before there is enough code to justify it. Keep the current flat structure while the application is still only a small shell.

## Directory Responsibilities

### app/

Application-level setup:

- application entrypoint
- router configuration
- application providers
- global application configuration

Do not place feature-specific UI or API logic here.

### pages/

Route-level screens:

- pages mapped to routes
- page-level composition
- coordination between multiple features

Pages compose features. They should not become large containers for backend integration or business-specific rendering details.

### features/

Feature-owned code:

```text
features/
|-- article/
|-- source/
`-- category/
```

A feature may contain:

```text
article/
|-- components/
|-- api/
|-- hooks/
`-- types.ts
```

Keep code here when it belongs to a single business feature, such as article lists, article details, category filters, source lists, or source filters.

### shared/

Code used by multiple features:

```text
shared/
|-- api/
|-- ui/
|-- hooks/
|-- utils/
`-- types/
```

Only move code into `shared/` after real reuse exists across multiple features.

### assets/

Static assets such as images, icons, and other files served by the frontend.

## API Integration Boundary

API-backed screens should use this direction of dependency:

```text
Page
  -> Feature hook
  -> Feature API function
  -> Shared API client
  -> Backend API
```

Example:

```text
pages/ArticleListPage
  -> features/article/hooks/useArticles
  -> features/article/api/articleApi
  -> shared/api/client
  -> backend
```

UI components should render data and handle user interaction. Backend communication belongs in feature API modules or shared API client code.

## Component Boundaries

Use three broad component levels:

```text
Page component
  -> Feature component
  -> Shared UI component
```

- Page components handle route-level composition and page-level states.
- Feature components handle business-specific UI and interactions.
- Shared UI components are reusable visual building blocks used by multiple features.

## State Boundary

Keep UI state close to the component or page that owns it.

Examples of server state:

- articles
- categories
- source companies

Examples of UI state:

- selected category
- search input
- opened dropdown
- selected page

Server state should flow through feature hooks and API modules. UI state should stay local unless a real cross-page requirement appears.

## Out Of Scope For This Document

The following rules belong to other documents and should not be duplicated here:

- project-wide MVP scope and simplicity rules: `AGENTS.md`
- backend, collector, classifier, and storage architecture: `docs/architecture.md`
- article and source domain rules: `docs/domain-model.md`
- company and category strategy: `docs/article-source-strategy.md`
- frontend API, coding, UI, and testing details: the matching files under `frontend/docs/`
