# Frontend AGENTS

Frontend-specific instructions for Global Tech Blog Archive.

The root `AGENTS.md` contains repository-wide rules. This file narrows those rules for frontend work.

Rule scope:

- root `AGENTS.md`: project-wide rules and common constraints
- root `docs/`: system architecture, domain model, source strategy, Git workflow, and documentation workflow
- `frontend/AGENTS.md`: frontend rules an AI agent must read before changing frontend files
- `frontend/docs/`: frontend architecture, UI, API, and testing details

## Always Follow

- Read the relevant frontend docs before implementation.
- Update this file when new frontend docs add or change working rules.
- Apply the root MVP/simplicity principles to frontend code.
- Prefer simple React and TypeScript patterns already used in this project.
- Do not mix frontend implementation work with unrelated backend changes, IDE files, or formatting-only changes.
- Avoid adding abstractions for future use cases that are not part of the current requirement.
- Prefer solving the current UI requirement over building reusable frameworks.

## AI-Delegated Frontend Work

Assume the user may not personally verify React, TypeScript, browser behavior, or UI regressions in detail.

When doing frontend work:

- build a verification path that a non-frontend user can trust
- prefer observable browser behavior over implementation claims
- keep Playwright coverage aligned with the user-facing workflow being changed
- report verification commands, results, and remaining UI risk clearly
- do not rely on visual inspection alone when Playwright can verify the behavior
- do not open an interactive browser or perform automated visual inspection unless the user explicitly requests it
- when manual confirmation is useful, run the required services and report the URL so the user can inspect it directly

Treat the frontend test harness as part of the feature surface. If a UI change cannot be confidently verified through the existing harness, improve the harness or explain the gap before calling the work complete.

## Frontend Responsibility

The frontend is responsible for:

- displaying article lists and article details
- displaying source and category information
- handling filtering, pagination, and navigation UI
- calling backend APIs
- handling loading, empty, and error states
- presenting data returned by the backend

The frontend is not responsible for:

- implementing backend business rules
- treating frontend validation as the source of truth for backend rules
- changing API contracts without discussion

## Frontend Docs

Read the docs that match the task before editing code. Some files may start empty and should be filled incrementally as frontend decisions are made:

```text
frontend/docs/architecture.md       # Frontend structure and component responsibilities
frontend/docs/coding-guidelines.md  # React and TypeScript implementation style
frontend/docs/api-guidelines.md     # API integration and data-fetching rules
frontend/docs/testing-guidelines.md # Frontend testing and verification rules
frontend/docs/ui-guidelines.md      # UI and UX implementation rules
```

For domain model, source strategy, and repository-wide Git workflow, also follow the root docs:

```text
docs/domain-model.md
docs/article-source-strategy.md
docs/git-workflow.md
docs/documentation-workflow.md
```

## Implementation Gate

Before frontend repo-tracked edits:

- Follow the root implementation gate and `docs/git-workflow.md`.
- For frontend documentation setup, prefer a `docs/<issue-number>/<topic>` branch.
- For frontend feature work, prefer a `feature/<issue-number>/<topic>` branch.

## Work Order

Prefer implementing frontend features in this order:

1. Confirm the page or user flow.
2. Confirm the backend API contract.
3. Define TypeScript types.
4. Add or adjust API client code.
5. Build UI components.
6. Handle loading, empty, and error states.
7. Run verification.

## API Rules

- Follow `frontend/docs/api-guidelines.md`.
- Ask before changing an API contract already used by the backend or frontend.

## Component Rules

- Follow `frontend/docs/coding-guidelines.md` and `frontend/docs/architecture.md`.

## Code Quality And Formatting

- Follow `frontend/docs/coding-guidelines.md` for ESLint and formatting rules.
- Run `npm run lint` after changing TypeScript, TSX, JavaScript, or ESLint configuration.
- Do not suppress ESLint errors or warnings merely to make the command pass. Fix the cause or explain why a rule change is needed.
- Do not perform repository-wide formatting as part of an unrelated feature or fix.
- Use `npm run format` instead of manually adjusting formatting style.
- Ask before changing the shared Prettier policy or introducing committed editor-specific settings.

## State Management Rules

- Follow `frontend/docs/coding-guidelines.md` and `frontend/docs/architecture.md`.

## Verification

Follow `frontend/docs/testing-guidelines.md`.

Before completing frontend code changes, run `npm run verify` from `frontend/` unless a narrower documentation-only check is sufficient.

When the change affects runtime behavior:

1. Run the frontend locally.
2. Report the local URL for user verification.
3. Verify behavior with non-interactive checks such as build, lint, and headless Playwright tests.

## Harness Evolution

When frontend verification becomes repetitive, propose improving the repository harness instead of relying on ad hoc agent behavior.

Suggest a Codex Skill, project automation hook, npm script, or Playwright helper when the same workflow repeats across multiple frontend changes, such as:

- running build, lint, and Playwright checks
- inspecting Playwright traces, screenshots, or videos after failures
- updating mocked API fixtures
- checking loading, empty, error, and success states
- verifying filters, pagination, and article links
- summarizing verification results and residual UI risk

A Skill or automation hook should not replace repository tests. It should standardize how AI agents run, inspect, and explain the frontend verification harness.

## Safety Gates

Ask before frontend changes that affect:

- shared API contracts
- routing structure
- major UI architecture changes
- dependency additions
- state management strategy
- application-wide styling approach

## Common Mistakes To Avoid

- implementing directly on `main`
- adding UI libraries without discussion
- changing backend API contracts from frontend work
- leaving unused code, debug code, or unrelated IDE changes in the work
