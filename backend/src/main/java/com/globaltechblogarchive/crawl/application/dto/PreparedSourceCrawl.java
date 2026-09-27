package com.globaltechblogarchive.crawl.application.dto;

import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import java.util.List;
import java.util.Map;

public record PreparedSourceCrawl(
        Long sourceId,
        List<ArticleCandidate> candidates,
        Map<String, ArticleAiDecision> decisionsByHash
) {

    public PreparedSourceCrawl {
        candidates = List.copyOf(candidates);
        decisionsByHash = Map.copyOf(decisionsByHash);
    }
}
