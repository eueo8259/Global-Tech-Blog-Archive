# Frontend UI Guidelines

This document defines UI implementation rules for Global Tech Blog Archive.

For frontend architecture and API integration rules, follow:

```text
frontend/docs/architecture.md
frontend/docs/api-guidelines.md
```

## Goal

This is not a design system document. It defines the minimum UI quality bar for the MVP.

Prefer clarity, predictability, and usability over visual complexity.

## API State Presentation

API-backed screens must make loading, empty, error, and success states visible and understandable.

Use `frontend/docs/api-guidelines.md` for the required API state coverage. Use this document for how those states should appear to users.

### Loading State

Do not show a blank screen while waiting for API data.

Use a visible loading treatment such as:

- `Loading...`
- `Loading articles...`
- skeleton UI

Users should be able to tell that the screen is waiting for data.

### Empty State

An empty state means the request succeeded but no matching data exists.

Use clear copy such as:

- `No articles found.`
- `Try adjusting the selected filters.`

Do not make an empty result look like a broken or unfinished screen.

### Error State

API failures must produce visible feedback.

Use concise copy that tells users the action failed, such as:

- `Failed to load articles.`
- `Please try again later.`

Do not leave stale loading UI on screen after a request fails.

### Success State

The success state should show the requested information clearly.

Examples:

- article list
- article detail
- source list
- category list

Do not optimize only for the success state while leaving other states unclear.

## Article Links

Article links are a primary action in this application.

When displaying an article:

- make the clickable title or action clear
- distinguish external article links when appropriate
- avoid hiding the destination behind unclear UI

Users should understand when a click will take them to an external article.

## Filters

Filters should be easy to discover and understand.

For category and source filters, make clear:

- which filters are active
- how to clear or change filters
- why the displayed results changed

Avoid hidden filtering behavior.

## Pagination

Pagination controls should be predictable.

Users should understand:

- which page or range they are viewing
- whether more results exist
- how to move between result pages

Avoid pagination behavior that changes unexpectedly after filtering or loading new data.

## Shared UI Components

Shared UI components may be useful for repeated UI patterns such as:

- buttons
- badges
- loading indicators
- empty states
- error messages

Follow `frontend/docs/architecture.md` for when a component should move into `shared/`.

## UI Completion Checklist

Before considering UI work complete, verify the affected screen has visible loading, empty, error, and success states.

Also verify that primary interactions such as article links, filters, and pagination are understandable for the user.
