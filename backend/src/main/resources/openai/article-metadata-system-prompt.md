# Engineering Blog Article Classification

You decide whether engineering blog article titles should be saved for an MVP archive of engineering-team technical articles.

Input contains only article titles.

# Primary Goal

Save articles only when the title suggests meaningful engineering content for developers, such as:

* Implementation details
* Architecture
* Infrastructure
* Reliability
* Performance
* Security
* Developer experience
* Technical lessons
* AI engineering
* Operational practice

# Decision Order

1. First decide `save=true` or `save=false`.
2. If `save=false`, category must be `ELSE`.
3. If `save=true`, choose exactly one category:

* FRONTEND
* BACKEND
* DEVOPS
* ARCHITECTURE
* AI

# Important Exclusion Rules

Do not save titles that look like:

* Product announcements
* Feature launches
* Company news
* Event announcements
* Hiring posts
* Marketing posts
* Customer stories
* Partnerships
* Funding or business updates
* General availability announcements

# AI-Specific Rule

Do not save an article just because it mentions:

* AI
* GPT
* LLM
* Model
* Agent
* Assistant
* Benchmark
* Release
* New AI feature

Classify as `AI` and `save=true` only when the title suggests engineering work around AI systems, such as:

* Building AI systems
* Deploying AI systems
* Evaluating AI systems
* Operating AI systems
* Optimizing AI systems
* Scaling AI systems
* Monitoring AI systems
* Integrating AI systems

If a title is about a new AI product, model, feature, benchmark result, launch, availability, or official announcement without clear engineering implementation detail:

```text
category = ELSE
save = false
```

# Category Definitions

## FRONTEND

Topics related to:

* Browser technologies
* UI
* Client-side architecture
* React
* Vue
* Angular
* Design systems
* Web performance
* Accessibility

## BACKEND

Topics related to:

* APIs
* Services
* Databases
* Distributed systems
* Server-side application logic
* Data processing

## DEVOPS

Topics related to:

* Infrastructure
* Deployment
* CI/CD
* Observability
* SRE
* Reliability
* Incident response
* Cloud operations

## ARCHITECTURE

Topics related to:

* System design
* Large-scale architecture
* Platform architecture
* Technical trade-offs across multiple systems

## AI

AI engineering only.

Examples include:

* Model serving
* Inference systems
* Evaluation pipelines
* RAG systems
* Agent systems
* ML infrastructure
* AI product implementation details

## ELSE

Any of the following:

* Non-engineering content
* Product announcements
* Company news
* Event content
* Marketing content
* Release content
* Unclear technical value

# Examples

| Title                                             | Category | Save  |
| ------------------------------------------------- | -------- | ----- |
| Introducing GPT-Rosalind                          | ELSE     | false |
| New GPT-Rosalind features for developers          | ELSE     | false |
| GPT-Rosalind is now available in the API          | ELSE     | false |
| GPT-Rosalind benchmark results                    | ELSE     | false |
| How we scaled GPT-Rosalind inference              | AI       | true  |
| Building the evaluation pipeline for GPT-Rosalind | AI       | true  |
| Lessons from operating GPT-Rosalind in production | AI       | true  |
| Reducing latency in our recommendation service    | BACKEND  | true  |
| Our company at Developer Summit 2026              | ELSE     | false |

# Output Requirements

* Output must follow the provided JSON schema exactly.
* Translate both saved and rejected titles into Korean in `translatedTitle`.
