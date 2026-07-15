# Frontend Testing Guidelines

This document defines frontend testing and verification rules for Global Tech Blog Archive.

For related frontend rules, follow:

```text
frontend/docs/architecture.md
frontend/docs/api-guidelines.md
frontend/docs/ui-guidelines.md
frontend/docs/coding-guidelines.md
```

## Goal

The goal is not to maximize test coverage.

The goal is to verify meaningful user-facing behavior and prevent regressions.

Frontend tests should answer:

- Does the UI show the correct state?
- Does user interaction work?
- Does the page behave correctly in a real browser?
- Can an AI agent verify the result through repeatable, non-interactive checks?

## Test Stack

Use Playwright as the primary frontend test tool.

Playwright is responsible for browser-based user flow verification. It should test the application from the user's perspective.

Do not introduce additional frontend testing libraries without discussion.

Vitest and React Testing Library are not part of the initial frontend test stack. Add them only when component-level tests become necessary and the dependency addition has been discussed.

## Playwright Scope

Use Playwright for behavior that is best verified in a real browser:

- page rendering
- article list rendering
- category filter interaction
- source filter interaction
- pagination interaction
- article link behavior
- loading, empty, error, and success states on important API-backed pages

Do not use Playwright for every small component. Prefer it for user flows and page-level behavior.

## API Strategy

Prefer mocked API responses in Playwright tests for frontend behavior.

Mocked API responses make it practical to verify:

- loading state
- empty state
- error state
- success state
- filtering behavior
- pagination behavior

Mock data must match the backend API contract. Do not invent response fields that do not exist.

Real backend integration can be added later as a small smoke-test layer when the MVP frontend and backend flow are stable.

## Required UI State Coverage

Important API-backed pages should verify all four states:

```text
Loading
Empty
Error
Success
```

Do not test only the success scenario.

At minimum, the article list page should verify:

- loading UI appears while data is being fetched
- empty UI appears when no data exists
- error UI appears when data loading fails
- success UI renders returned articles correctly

## What To Test

Add or update Playwright tests when changing:

- API-backed screens
- loading, empty, error, or success states
- filtering behavior
- pagination behavior
- article link behavior
- reusable UI patterns that affect user behavior
- bug fixes

Bug fixes should include a regression test when practical.

## What Not To Test

Avoid tests that only verify implementation details.

Do not test:

- private component state
- exact internal function calls
- CSS class names unless they are part of behavior
- framework or library behavior
- trivial rendering with no user-facing value

Good test names describe visible behavior:

```text
shows empty message when no articles are returned
shows error message when article loading fails
updates article list when category filter is selected
opens article link when article card is clicked
```

Avoid vague names:

```text
renders component
works correctly
test article list
```

## Locator Rules

Write Playwright tests from the user's perspective.

Prefer:

- locating by role
- locating by accessible name
- locating by label
- locating by visible text
- clicking actual buttons and links

Avoid fragile selectors such as CSS classes unless no user-facing selector is available.

Examples:

```typescript
await page.getByRole('button', { name: 'Next' }).click();
await expect(page.getByText('No articles found.')).toBeVisible();
await expect(page.getByRole('link', { name: /engineering/i })).toBeVisible();
```

## Browser Coverage

Use Chromium as the default local Playwright browser.

Do not require Firefox and WebKit for the initial MVP unless a specific browser issue appears or CI coverage is intentionally expanded.

## Test Location

Prefer this structure:

```text
frontend/
|-- e2e/
|   `-- article-list.spec.ts
`-- playwright.config.ts
```

Keep tests organized by page or user flow.

## Verification Commands

Run from `frontend/`.

Required before completion:

```bash
npm run verify
```

The `verify` script runs build, lint, and Playwright checks in sequence. During implementation, run the narrower command that proves the current change, then run `npm run verify` before completion.

Code quality and formatting policy belongs in `frontend/docs/coding-guidelines.md`. This document only defines when verification commands and browser checks are required.

Playwright scripts:

```json
{
  "test:e2e": "playwright test",
  "test:e2e:install": "playwright install chromium",
  "test:e2e:ui": "playwright test --ui",
  "test:e2e:headed": "playwright test --headed"
}
```

Configure Playwright to start the Vite dev server through `webServer` so `npm run test:e2e` can run without manually starting `npm run dev`.

If Chromium is missing in a new local environment, run:

```bash
npm run test:e2e:install
```

## Manual Verification

Do not open an interactive browser or perform automated visual inspection unless the user explicitly requests it.

When runtime UI behavior changes, run the required services and report the URL so the user can verify the affected page or flow directly.

Check:

- loading state
- empty state
- error state
- success state
- filter behavior
- pagination behavior
- article link behavior
- browser console errors

Headless Playwright coverage remains useful and does not require opening a visible browser window.

## Completion Rule

Do not consider frontend work complete if:

- build fails
- lint fails
- relevant Playwright tests are missing or failing
- the UI only handles the success state
- runtime behavior changed but the affected flow has no automated verification or user-verifiable URL

`npm run verify` includes `format:check` before lint, build, and Playwright tests.

If Playwright is not available in a fresh environment, run `npm install` and `npm run test:e2e:install` before treating `npm run test:e2e` as an available verification command.

## Harness Evolution Signals

Because frontend work may be mostly delegated to AI agents, improve the verification harness whenever repeated manual judgment appears.

Propose a shared helper, fixture, npm script, Codex Skill, or project automation hook when the same verification workflow repeats across multiple changes.

Good candidates include:

- standard API mocks for article success, empty, error, loading, filtering, and pagination cases
- a shared Playwright console-error guard
- a shared page object or helper for common article-list interactions
- a `verify` script that runs build, lint, and Playwright checks together
- a Skill that runs the frontend verification loop and explains failures using traces or screenshots
- an automation hook that reminds agents to run the required frontend verification before completion

Do not add automation for a one-off case. Prefer automation when it makes the frontend safer for a user who is not personally reviewing React or browser behavior in detail.
