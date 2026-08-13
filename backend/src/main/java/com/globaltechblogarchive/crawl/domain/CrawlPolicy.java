package com.globaltechblogarchive.crawl.domain;

import java.util.Objects;

public record CrawlPolicy(
        CrawlMode mode,
        int maxCandidatesPerSource
) {

    public static final int RECENT_MAX_CANDIDATES = 20;
    public static final int BACKFILL_MAX_CANDIDATES = 50;

    public CrawlPolicy {
        Objects.requireNonNull(mode, "mode must not be null");
        if (maxCandidatesPerSource < 1) {
            throw new IllegalArgumentException("maxCandidatesPerSource must be greater than 0");
        }
    }

    public static CrawlPolicy recent() {
        return new CrawlPolicy(CrawlMode.RECENT, RECENT_MAX_CANDIDATES);
    }

    public static CrawlPolicy backfill(int maxCandidatesPerSource) {
        return new CrawlPolicy(CrawlMode.BACKFILL, maxCandidatesPerSource);
    }
}
