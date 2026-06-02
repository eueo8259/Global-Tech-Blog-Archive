# Article Source Strategy

## Goal

Collect engineering articles from global technology company blogs for developers who want technical implementation details, architecture decisions, operating lessons, or AI engineering practices.

The MVP target article types are product engineering, large-scale systems, infrastructure, architecture, backend/frontend implementation, DevOps, and AI engineering.

## Initial Companies

The MVP collects articles only from the following companies:

| Company | Main Topics |
| --- | --- |
| OpenAI | AI, Agentic AI, Engineering, Safety, Security |
| Anthropic | Agentic AI, Evals, Context Engineering, AI Safety, Developer Tools |
| Netflix | Distributed Systems, Streaming, Data Platform, Personalization, Reliability |
| Figma | Infrastructure, Realtime Collaboration, Frontend Performance, Database/Storage, Developer Experience |
| Meta | Infrastructure, AI/ML, Mobile, Data Infrastructure, Security, Open Source |
| Uber | Backend, Data/ML, Realtime Systems, Maps, Optimization, Security |
| Airbnb | Infrastructure, Data, AI/ML, Search, Payments, Mobile/Web |
| Stripe | Payments, API Design, Database, Developer Experience, Risk/Fraud, Infrastructure |
| Cloudflare | Network, Security, Edge Computing, Reliability, Infrastructure, Open Source |
| GitHub | Developer Experience, Platform Engineering, Security, Search, AI/Copilot, Frontend |
| LinkedIn | Search, Feed, Recommendations, AI, Data Streaming, Infrastructure |
| DoorDash | Logistics, Experimentation, Data Platform, Backend, ML, Reliability |
| Discord | Realtime Systems, Messaging, Voice, Elixir/Rust, Data, Reliability |
| Shopify | Commerce Platform, AI/ML, Search, Infrastructure, Mobile, Ruby/Rails |
| Datadog | Observability, SRE, Metrics/Logs/Traces, Security, Data Platform, AI Observability |
| Slack | Infrastructure, Reliability, DevOps, Collaboration Platform, Developer Experience |
| Amazon Science | AI/ML, Robotics, Search, Optimization, Cloud Infrastructure, Applied Research |

Do not add more companies during the MVP unless explicitly requested.

## MVP Category Filter

Articles have one primary category.

```text
ALL
Frontend
Backend
Devops
Architecture
AI
Else
```

`ALL` is a UI/API filter option only. It must not be stored as an article category.

## Category Rules

Use a single representative category per article for the MVP.

Keyword mapping:

| Category | Example Signals |
| --- | --- |
| Frontend | frontend, front-end, web, browser, UI, UX, CSS, design system, React, TypeScript, JavaScript |
| Backend | backend, API, server, database, storage, cache, queue, Kafka, MySQL, Postgres, Redis, Go, Java, Ruby, Python |
| Devops | DevOps, SRE, incident, monitoring, observability, CI/CD, deployment, Kubernetes, Docker, Terraform, infrastructure, cloud |
| Architecture | architecture, distributed systems, scalability, migration, platform, reliability, availability, latency, performance, system design |
| AI | AI, ML, machine learning, LLM, agent, model, embedding, recommendation, ranking, search, eval, inference, training |
| Else | culture, hiring, product update, company news, event, or articles that do not match another category |

Classification priority:

```text
AI -> Devops -> Architecture -> Backend -> Frontend -> Else
```

## Collection Strategy

1. Use RSS or Atom feeds as the default collection method.
2. Use HTML list-page parsing only when one of these conditions is true:
   - the source does not provide an RSS or Atom feed
   - the feed omits one or more MVP-required fields: title, original URL, published date, summary, or company/source identity
   - the feed contains only product/news entries while the source has a separate engineering article list page
3. Store title, original URL, normalized URL, summary, company, published date, collected date, and category.
4. Do not fetch article detail pages only to discover canonical URLs in the MVP.

## Future Considerations

Do not implement these items before article collection, category filtering, and latest-first listing are working:

- Multiple topic tags per article
- Quality scoring
- Full-text extraction
- Search
- Company/source management UI
- Personalized feeds
