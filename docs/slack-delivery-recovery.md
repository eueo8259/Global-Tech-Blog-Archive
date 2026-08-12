# Slack Delivery Recovery

## Purpose

Daily Digest delivery uses a stable `delivery_key` in Slack message metadata and
`conversations.history` to reconcile ambiguous send results before retrying.
This is recovery close to effectively-once delivery. Slack does not provide an
idempotency key for this flow, so this is not mathematical Exactly Once.

Metadata is used only as a correlation key for lookup. It does not make Slack
deduplicate repeated `chat.postMessage` calls.

## Required Bot Scopes

The Slack app requires all existing scopes plus:

- `channels:history` for public channels
- `groups:history` for private channels
- `metadata.message:read` for reading the delivery key from message metadata

The bot must be a member of every channel whose messages it verifies. Changing
`SLACK_BOT_SCOPES` changes the next OAuth request only; it does not add scopes to
tokens already issued.

After scope configuration changes:

1. Add both history scopes and `metadata.message:read` in the Slack app configuration.
2. Add the metadata event schema shown below.
3. Reinstall the app to each existing workspace.
4. Confirm the bot is a member of each subscribed public or private channel.
5. Confirm the newly stored workspace scope includes all three new scopes.

Do not change the remote Slack app or reinstall it through automation without
explicit operator approval.

## Metadata Event Schema

Register this custom metadata event schema in the Slack app Manifest:

```yaml
events:
  techport_digest_sent:
    title: TechPort Digest Sent
    description: A TechPort Daily Digest message was sent
    type: object
    properties:
      delivery_key:
        type: string
    required:
      - delivery_key
```

Daily Digest messages send this payload:

```json
{
  "metadata": {
    "event_type": "techport_digest_sent",
    "event_payload": {
      "delivery_key": "2d52cf60-8d79-48fc-9807-d53c51e9170d"
    }
  }
}
```

Slack references:

- [Message metadata](https://docs.slack.dev/messaging/message-metadata/)
- [Designing a metadata event schema](https://docs.slack.dev/messaging/message-metadata/designing-metadata-event-schema/)
- [`conversations.history`](https://docs.slack.dev/reference/methods/conversations.history/)

## Recovery Policy

- Ambiguous sends and stale `PROCESSING` rows become `VERIFYING`.
- A matching metadata delivery key, or an existing known message timestamp,
  confirms `SENT`.
- Three complete History scans that find no message allow resend.
- Each verification reads at most one History page. The cursor and original
  upper time bound are stored so rate-limited apps resume on a later run
  without restarting from the first page.
- HTTP 429, network, Slack service, authentication, and permission failures do
  not count as message absence.
- Permission failures use a one-hour recheck delay and never trigger automatic
  resend while the permission problem remains.
- A later Daily Digest for the same channel is blocked while a delivery remains
  `VERIFYING`.

## Remaining Limits

- A deleted message or one outside Slack retention can eventually be resent.
- Metadata visibility delay is reduced by repeated checks, not eliminated.
- Removed permissions prevent confirmation, so the delivery remains verifying.
- Multiple application instances claiming the same work are outside the current
  single-instance design.
