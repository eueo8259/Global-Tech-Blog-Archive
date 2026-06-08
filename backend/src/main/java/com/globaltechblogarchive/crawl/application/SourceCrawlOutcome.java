package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;

public record SourceCrawlOutcome(
        SourceCrawlResult result,
        CrawlRunSummary summary
) {
}
