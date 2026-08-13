package com.globaltechblogarchive.crawl.api;

import com.globaltechblogarchive.crawl.api.dto.CrawlSummaryResponse;
import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "dev"})
@RestController
@RequiredArgsConstructor
public class ArticleCrawlController {

    private final ArticleCrawlService articleCrawlService;

    @PostMapping("/api/admin/article-crawls/scheduled-run")
    public CrawlSummaryResponse runScheduled() {
        return CrawlSummaryResponse.from(articleCrawlService.runScheduled());
    }

    @PostMapping("/api/admin/article-crawls/sources/{sourceKey}/backfill-run")
    public CrawlSummaryResponse runSourceBackfill(
            @PathVariable String sourceKey,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return CrawlSummaryResponse.from(articleCrawlService.runSourceBackfill(sourceKey, limit));
    }

    @PostMapping("/api/admin/article-crawls/backfill-run")
    public CrawlSummaryResponse runAllBackfill(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return CrawlSummaryResponse.from(articleCrawlService.runAllBackfill(limit));
    }

    @PostMapping("/api/admin/article-crawls/ai-failures/retry")
    public CrawlSummaryResponse retryAiFailures(@RequestParam(defaultValue = "20") int limit) {
        return CrawlSummaryResponse.from(articleCrawlService.retryAiFailures(limit));
    }
}
