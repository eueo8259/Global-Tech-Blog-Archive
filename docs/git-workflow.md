## Git Workflow

This workflow is an implementation gate, not just a commit convention.

Before code implementation or repo-tracked file edits for a feature/change:

1. Check the current branch.
2. Do not implement on `main`.
3. Create or confirm a GitHub Issue.
4. Create a working branch from `develop`.
5. Proceed only after the branch matches the issue-based branch naming rules below.

Planning discussion alone does not require a branch.
Repository documentation, backend/frontend code, DB schema, dependencies, migrations, and tests do require this gate when they are part of an implementation change.

### Branch Strategy

* `develop` is the default branch and primary integration branch.
* `main` is the production-ready release branch.
* Create all working branches from `develop`.
* Merge working branches into `develop` through Pull Requests.
* Merge `develop` into `main` when a release is ready.

### Issue-Based Development

Before starting any work:

1. Create a GitHub Issue.
2. Create a branch from `develop`.
3. Implement the change.
4. Create a Pull Request linked to the Issue.
5. Merge the Pull Request into `develop`.
6. Verify the linked Issue is closed after the Pull Request is merged into `develop`.

`develop` is the completion point for issue work in this repository.
Because `develop` is the default branch, GitHub issue closing keywords in PR bodies should close linked issues when PRs merge into `develop`. After merge, verify the linked Issue state and close it manually only if GitHub did not close it automatically.

### Branch Naming

Format:

```text
<type>/<issue-number>/<topic>
```

Examples:

```text
feature/12/article-list
feature/15/rss-collector
fix/18/parser-error
docs/22/domain-model
refactor/30/article-service
```

Allowed types:

* `feature`
* `fix`
* `docs`
* `refactor`
* `test`
* `chore`

Rules:

* Every branch must be linked to a GitHub Issue.
* Use short, descriptive topics.
* Use lowercase and kebab-case.
* Validate branch names before creating or switching branches:

```text
^(feature|fix|docs|refactor|test|chore)/[0-9]+/[a-z0-9]+(-[a-z0-9]+)*$
```

### Commit Messages

Use Conventional Commits:

```text
feat: add article list API
fix: resolve RSS parsing error
docs: update domain model
refactor: simplify article service
test: add article service tests
chore: update dependencies
```

### Pull Requests

* Create one Pull Request per Issue.
* Keep Pull Requests focused on a single purpose.
* Do not mix features, refactoring, and formatting changes in the same Pull Request.
* Include a summary of what changed and why.

Pull Request titles must use the same Conventional Commit style as commit messages, for example `feat: collect article candidates by source` or `refactor: reorganize crawl architecture`. Do not add tool or agent prefixes such as `[codex]`, `[ai]`, or similar labels to PR titles unless explicitly requested.

Validate PR titles before creating or updating Pull Requests:

```text
^(feat|fix|docs|refactor|test|chore)(\([^)]+\))?: .+
```

When creating a Pull Request through any automation, connector, CLI, or AI agent, first read `.github/PULL_REQUEST_TEMPLATE.md` if it exists. Use that template as the PR body structure instead of writing a custom summary. Fill the related issue, summary, motivation, changes, notes, and verification fields. After creation, verify the PR title, base branch, draft state, related issue section, and body format match repository conventions.

When creating a PR through an automation, connector, CLI, or AI agent, do not assume the GitHub Pull Request template was applied automatically. Inspect or construct the PR body explicitly and verify that the repository template structure is present before considering PR creation complete.

Because issue work completes when the PR is merged into `develop`, check the linked Issue after the merge. If the Issue remains open and the PR completed its scope, close it manually with the completed reason.

### Automation Checklist

Before any automated branch, commit, push, or PR action:

1. Read this workflow file and `.github/PULL_REQUEST_TEMPLATE.md` if PR work is involved.
2. Print or state the issue number, base branch, intended branch name, commit message, PR title, PR base, and draft state.
3. Validate the branch name and PR title against the regex rules above.
4. Confirm no plugin, connector, or agent default naming convention overrides repository rules.
5. Stop before writes if any value is missing, malformed, or conflicts with this workflow.

After any automated branch, commit, push, or PR action:

1. Re-check the current branch and upstream branch.
2. Re-check the PR title, base branch, draft state, and body template headings.
3. Verify the related issue section is filled with the actual issue number.
4. Fix mismatches immediately before reporting completion.
