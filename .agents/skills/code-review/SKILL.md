---
name: code-review
description: Review the current branch against its base without modifying files, and return prioritized findings plus a pre-PR verdict. Use when the user asks for a code review, pre-PR review, branch diff review, or an independent review before creating a pull request.
---

# Code Review

Perform an independent, read-only review of the current change before a pull
request is created. Focus on defects that can affect behavior, safety, or the
stated requirements.

## Establish the Review Scope

1. Read the repository `AGENTS.md` and every applicable lower-scope
   `AGENTS.md` for changed files.
2. Read the repository review and Git workflow documents when they exist.
3. Identify the requested behavior from the user, linked Issue, specification,
   or branch context. State any material assumption.
4. Use the base branch named by the user. Otherwise use the repository's
   documented integration branch, falling back to the remote default branch.
5. Compare from the merge base to the current working tree. Include committed,
   staged, unstaged, and relevant untracked files in the review scope.
6. Stop and ask one focused question when the intended change or base cannot be
   determined safely.

## Review the Change

1. Inspect the complete diff before judging individual files.
2. Read enough surrounding production code and tests to validate each suspected
   issue.
3. Prioritize functional correctness, regressions, data integrity, security,
   concurrency, transaction boundaries, external side effects, public
   contracts, and missing tests for observable behavior.
4. Check that every finding is introduced by the reviewed change and is
   reproducible under a concrete condition.
5. Leave formatting, lint, and other deterministic checks to automated tools.
6. Avoid speculative hardening, subjective style preferences, and unrelated
   refactoring suggestions.
7. Do not modify files, stage changes, create commits, push branches, or create
   pull requests. Do not implement fixes as part of this review.

Use read-only inspection commands. Run tests only when the user explicitly asks
for them as part of the review; otherwise report the available verification
evidence and any resulting uncertainty.

## Classify Findings

- `P0`: An immediate critical risk such as data loss, severe security exposure,
  or broad service outage.
- `P1`: A merge-blocking defect such as incorrect behavior, a significant
  regression, or a required behavior that is missing.
- `P2`: A concrete, non-blocking maintainability or test weakness with a clear
  future cost. Omit low-value nits.

Sort findings by priority, then by file location. For every finding include:

- a concise action-oriented title;
- an exact file and the smallest useful line range;
- the condition that triggers the problem;
- the user or system impact;
- a brief safe direction when it materially helps the implementer.

## Return the Review

Lead with findings. Use the user's language.

When findings exist, return:

```text
Findings

[P1] Concise title
path/to/file:line
Trigger and impact, followed by a brief safe direction when useful.

Verdict: CHANGES REQUIRED
```

When no findings exist, return:

```text
No findings.

Verdict: PASS
```

Use `CHANGES REQUIRED` when any `P0` or `P1` finding remains. `P2` findings do
not block a `PASS`, but list them before the verdict. After the verdict, mention
only material assumptions, verification gaps, or residual risks.
