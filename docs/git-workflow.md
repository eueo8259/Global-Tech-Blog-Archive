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

* `main` is the production-ready branch.
* `develop` is the primary integration branch.
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
6. Close the linked Issue when the Pull Request is merged into `develop`.

`develop` is the completion point for issue work in this repository.
Because GitHub only auto-closes issues from closing keywords when a Pull Request is merged into the repository default branch, do not rely on `Closes #<issue>` alone while `main` remains the default branch.
After a Pull Request is merged into `develop`, close the linked Issue manually if GitHub did not close it automatically.

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
* Link the Pull Request to the Issue in the PR body.
* When the Pull Request is merged into `develop`, ensure the linked Issue is closed.

Example:

```text
Issue: #12
```
