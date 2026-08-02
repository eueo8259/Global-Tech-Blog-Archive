package com.globaltechblogarchive.crawl.application.dto;

public record AiReviewRunResult(
        int candidateCount,
        int approvedCount,
        int rejectedCount,
        int failedCount,
        int retryWaitingCount,
        int storedCount,
        int recoveredCount
) {

    public static AiReviewRunResult empty(int recoveredCount) {
        return new AiReviewRunResult(0, 0, 0, 0, 0, 0, recoveredCount);
    }

    public AiReviewRunResult plus(AiReviewRunResult other) {
        return new AiReviewRunResult(
                candidateCount + other.candidateCount,
                approvedCount + other.approvedCount,
                rejectedCount + other.rejectedCount,
                failedCount + other.failedCount,
                retryWaitingCount + other.retryWaitingCount,
                storedCount + other.storedCount,
                recoveredCount + other.recoveredCount
        );
    }
}
