## Git Workflow

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
6. Close the Issue automatically when the Pull Request is merged.

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
* Link the Pull Request to the Issue using GitHub closing keywords.

Example:

```text
Closes #12
Fixes #15
Resolves #18
```
