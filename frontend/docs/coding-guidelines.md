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

## Code Quality Tools

Code quality checks and code formatting solve different problems.

### ESLint

ESLint analyzes source code before runtime and reports suspicious or disallowed patterns. It helps detect problems such as unused variables, invalid hook usage, and unsafe React refresh exports.

In Java terms, its role overlaps with tools such as Checkstyle, SpotBugs, and static analysis rules in SonarQube. It is not a replacement for TypeScript compilation or browser tests.

This project already uses ESLint. The active configuration is `frontend/eslint.config.js`, and the command is:

```bash
npm run lint
```

The current configuration applies:

- recommended JavaScript rules
- recommended TypeScript ESLint rules
- React Hooks rules
- React Refresh export rules
- browser globals for TypeScript and TSX files
- exclusion of generated `dist/` output

Treat lint findings as code issues to understand, not obstacles to bypass. Do not add disable comments, weaken a rule, or ignore a file without a concrete reason.

### Prettier And Formatting

Prettier is a formatter. It rewrites whitespace, line breaks, quotes, and similar presentation details so contributors produce a consistent layout.

Prettier does not replace ESLint:

- ESLint checks code quality and problematic patterns.
- Prettier standardizes code appearance.

Prettier is not currently installed or configured in this project. There is no `format` script, so do not claim that formatting was verified with Prettier and do not assume editor save actions will format code.

Adding Prettier requires prior discussion because it adds dependencies and establishes a project-wide formatting policy. A future adoption should define, in one focused change:

- the Prettier version and configuration
- ignored generated files
- a `format` command that writes changes
- a `format:check` command suitable for verification or CI
- how ESLint and Prettier avoid conflicting formatting rules
- whether existing files are reformatted separately from feature work

Until then, preserve the style of nearby files and keep formatting changes limited to lines touched by the task.

For this project, the agreed adoption target is:

```json
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "printWidth": 100,
  "trailingComma": "all",
  "endOfLine": "lf"
}
```

Use `printWidth: 100` because React and TypeScript expressions commonly contain longer props and type names. This keeps code readable without the frequent wrapping produced by 80 columns.

When Prettier is introduced, exclude generated, dependency-owned, sensitive, and compressed files through `.prettierignore`, including:

```text
node_modules/
dist/
build/
coverage/
.vite/
.env
.env.*
*.log
*.min.js
package-lock.json
playwright-report/
test-results/
```

Use `eslint-config-prettier` as the final ESLint configuration so ESLint does not enforce formatting rules that conflict with Prettier. Do not add `eslint-plugin-prettier`; run formatting and linting as separate commands so failures retain a clear owner.

### Editor Setup

Document editor convenience here rather than committing `.vscode/`, which is ignored by this repository.

For VS Code, recommend the official Prettier extension and these user or workspace settings after Prettier is installed:

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode"
}
```

Editor automation is optional convenience. Repository commands remain the source of truth.

### Adoption And Future Automation

Introduce Prettier in a dedicated issue and PR without feature or API changes. Separate configuration changes from mechanical source formatting into distinct commits, state that the formatting commit contains no logic changes, and inspect `git diff -w` before completion.

Do not add Husky or lint-staged initially. If contributors repeatedly forget formatting checks, consider them in a separate issue for checking or formatting staged frontend files before commit.

## Tool Responsibilities

Use each command for its own purpose:

| Command | Responsibility |
| --- | --- |
| `npm run build` | Type-check TypeScript and produce the Vite production build |
| `npm run lint` | Run ESLint static analysis |
| `npm run test:e2e` | Verify user-visible browser behavior with Playwright |
| `npm run verify` | Run build, lint, and Playwright checks together |

There is currently no formatting command. A successful build does not prove lint or browser behavior, and successful lint does not prove type correctness or UI behavior.

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
function ArticleCard({ title, companyName }: ArticleCardProps) {
  return (
    <article>
      <h2>{title}</h2>
      <p>{companyName}</p>
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
  companyName: string;
}

function ArticleCard({ title, companyName }: ArticleCardProps) {
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
- ESLint disable comments added only to silence findings
- broad formatting changes mixed into feature work
- claiming Prettier formatting when the project has no Prettier setup
