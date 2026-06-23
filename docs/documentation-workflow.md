# Documentation Workflow

## Notion Source

Long-form domain design notes are tracked in Notion:

- Domain Design Notes: https://www.notion.so/3737f30e2bdc8079907ceb5525bb6ef6

## When To Update Notion

Update the Notion page when changing:

- domain entities
- DB schema
- URL and deduplication policy
- collection source rules
- category rules
- crawler or scheduler behavior that affects domain decisions

## Workflow

1. Read the Notion page before making domain-related changes.
2. Update repo docs when implementation-facing rules change.
3. Update Notion when domain decisions or learning notes change.
4. Report whether Notion was updated in the final response.

## AGENTS.md Files

Use `AGENTS.md` files as concise routing and guardrail documents.

- Root `AGENTS.md` should stay short, around 60-70 lines.
- Keep repository-wide principles in root `AGENTS.md`.
- Move detailed workflow rules to `docs/git-workflow.md`.
- Move backend implementation rules to `backend/AGENTS.md` or `backend/docs/`.
- Do not duplicate long command lists, PR rules, or scope-specific rules in root `AGENTS.md`.
- When root `AGENTS.md` grows, move detailed guidance to the narrowest relevant doc and leave a pointer.

## If Notion Is Unavailable

If Notion cannot be accessed, provide the exact replacement text for manual update.
