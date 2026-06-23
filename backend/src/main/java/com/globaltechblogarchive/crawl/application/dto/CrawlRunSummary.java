package com.globaltechblogarchive.crawl.application.dto;

import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import java.util.List;

public record CrawlRunSummary(
        int discoveredCount,
        int duplicateCount,
        int storedCount,
        int aiApprovedCount,
        int aiRejectedCount,
        int aiFailedCount,
        int previouslyApprovedCount,
        int previouslyRejectedCount
) {

    public static CrawlRunSummary empty() {
        return new CrawlRunSummary(0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static CrawlRunSummary from(ProcessedCandidates processed) {
        List<ArticleCandidate> candidates = processed.candidates();
        return new CrawlRunSummary(
                candidates.size(),
                countDuplicates(candidates),
                processed.storedArticleCount(),
                countStatus(candidates, ArticleCandidateDecisionStatus.AI_APPROVED),
                countStatus(candidates, ArticleCandidateDecisionStatus.AI_REJECTED),
                countStatus(candidates, ArticleCandidateDecisionStatus.AI_FAILED),
                countStatus(candidates, ArticleCandidateDecisionStatus.PREVIOUSLY_APPROVED),
                countStatus(candidates, ArticleCandidateDecisionStatus.PREVIOUSLY_REJECTED)
        );
    }

    public CrawlRunSummary plus(CrawlRunSummary other) {
        return new CrawlRunSummary(
                discoveredCount + other.discoveredCount,
                duplicateCount + other.duplicateCount,
                storedCount + other.storedCount,
                aiApprovedCount + other.aiApprovedCount,
                aiRejectedCount + other.aiRejectedCount,
                aiFailedCount + other.aiFailedCount,
                previouslyApprovedCount + other.previouslyApprovedCount,
                previouslyRejectedCount + other.previouslyRejectedCount
        );
    }

    public int candidateCount() {
        return discoveredCount;
    }

    private static int countDuplicates(List<ArticleCandidate> candidates) {
        return (int) candidates.stream().filter(ArticleCandidate::duplicate).count();
    }

    private static int countStatus(List<ArticleCandidate> candidates, ArticleCandidateDecisionStatus status) {
        return (int) candidates.stream()
                .filter(candidate -> !candidate.duplicate())
                .filter(candidate -> candidate.decisionStatus() == status)
                .count();
    }
}
