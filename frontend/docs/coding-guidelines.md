# Frontend Coding Guidelines

This document defines React and TypeScript implementation rules for Global Tech Blog Archive.

For architecture, API integration, and UI behavior, follow:

```text
frontend/docs/architecture.md
frontend/docs/api-guidelines.md
frontend/docs/ui-guidelines.md
```

## Goal

Keep frontend code simple, predictable, easy to modify, and easy for future contributors and AI agents to understand.

Optimize first for local understanding. Introduce reuse when duplication becomes real and the shared shape is clear.

## General Principles

Prefer:

- explicit code
- local reasoning
- feature ownership
- straightforward React and TypeScript patterns

Avoid:

- premature abstraction
- unnecessary indirection
- framework-like code inside the application
- abstractions created only for hypothetical future reuse

## React Components

Use function components.

Prefer function declarations for named components unless the existing file already uses another consistent style.

```tsx
function ArticleCard({ title, sourceCompanyName }: ArticleCardProps) {
  return (
    <article>
      <h2>{title}</h2>
      <p>{sourceCompanyName}</p>
    </article>
  );
}
```

Function declarations are easy to identify, debug, and navigate. Still, consistency with nearby code is more important than changing style for its own sake.

## Component Responsibility

Each component should have one primary responsibility.

Good component names usually describe visible UI or a clear interaction:

```text
ArticleCard
ArticleList
CategoryFilter
SourceFilter
```

Avoid components that coordinate unrelated concerns:

```text
ArticlePageManager
GlobalArticleContainer
```

If a component starts mixing API calls, data transformation, layout, and unrelated interactions, split the responsibilities.

## Props

Define explicit props interfaces for components that receive props.

```tsx
interface ArticleCardProps {
  title: string;
  sourceCompanyName: string;
}

function ArticleCard({ title, sourceCompanyName }: ArticleCardProps) {
  return <h2>{title}</h2>;
}
```

Use destructuring when it improves readability. Keeping `props` as an object is also fine when many props are passed through together.

Do not use `any` for component props.

## TypeScript

Prefer precise types that describe the data the component or function actually uses.

Use interfaces or type aliases for:

- component props
- API response objects
- feature-owned data structures
- non-obvious UI state

Keep feature-specific types close to the feature that owns them:

```text
features/article/types.ts
```

Move types into `shared/` only after multiple features actually use them.

Avoid `any`. For untrusted or unknown external values, prefer `unknown` and narrow the value before using it.

## Hooks

Create a custom hook only when it removes meaningful complexity.

Good reasons to create a hook:

- API fetching and related state are easier to read behind a hook
- URL query state needs to be synchronized with UI state
- interaction logic has grown large enough to distract from rendering
- the same logic is reused by more than one component

Good examples:

```text
useArticles()
useCategoryFilter()
```

Avoid hooks that only wrap a small amount of local component state:

```text
useArticleCard()
```

Hooks are abstractions. They should make code easier to understand, not just move code to another file.

## State Handling

Prefer local component state first.

Do not create state for values that can be derived from props, server data, or existing state.

Keep server state and UI state separate:

- server state: articles, categories, source companies
- UI state: selected category, search input, opened dropdown, selected page

Avoid adding global state or state management libraries without a concrete requirement and prior discussion.

## Conditional Rendering

Prefer early returns for page-level UI states.

```tsx
if (isLoading) {
  return <LoadingIndicator />;
}

if (error) {
  return <ErrorMessage />;
}

if (articles.length === 0) {
  return <EmptyState />;
}

return <ArticleList articles={articles} />;
```

Avoid deeply nested conditional rendering in JSX when separate early returns would be easier to read.

## API Usage

Do not call backend APIs directly from deeply nested UI components.

Follow `frontend/docs/api-guidelines.md` for API communication rules and `frontend/docs/architecture.md` for module boundaries.

## File Organization

Keep code inside the owning feature by default.

Split a file when it starts handling multiple responsibilities, such as:

- component rendering plus API calls
- component rendering plus large data transformation
- multiple unrelated UI concerns

Do not split files only to satisfy a line-count preference. Responsibility is more important than file length.

## What To Avoid

Avoid:

- `any`
- deeply nested conditionals
- business logic inside UI components
- API calls inside deeply nested components
- shared abstractions created before real reuse exists
- new frontend dependencies without discussion
