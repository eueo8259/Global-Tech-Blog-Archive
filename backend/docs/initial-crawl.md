# Initial Article Crawl

The local-only initial crawl endpoint collects up to 20 candidates from every
enabled blog source, sends new candidates through AI review, and stores the
results in the local database.

```http
POST /api/admin/article-crawls/initial-run
```

Unlike the regular `/api/admin/article-crawls/run` endpoint, the initial crawl
does not apply the two-day publication window. Candidates with publication
dates are sorted newest first. Candidates without a parsed date fill remaining
positions after dated candidates, preserving source order.

The response contains run and source summaries only. Inspect detailed results
in `articles`, `article_ai_decisions`, `article_collections`, and
`article_collection_runs`.

## Long-Running Request

The endpoint processes external sources and AI calls synchronously and can take
several minutes. Use a client with an explicit timeout instead of a browser.

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/admin/article-crawls/initial-run" `
  -Method POST `
  -TimeoutSec 1800
```

If the client disconnects, check application logs and the collection run tables
before retrying. Do not start another initial crawl until the previous request
has finished.

Each source is committed in its own transaction. A failed source is rolled back
and recorded without removing data committed for earlier sources.
