package com.globaltechblogarchive.crawl.application.dto;

public record AiReviewCandidateResult(
        int approvedCount,
        int rejectedCount,
        int failedCount,
        int retryWaitingCount,
        int storedCount
) {

    public static AiReviewCandidateResult approved(boolean stored) {
        int storedCount = 0;
        if (stored) {
            storedCount = 1;
        }
        return new AiReviewCandidateResult(1, 0, 0, 0, storedCount);
    }

    public static AiReviewCandidateResult rejected() {
        return new AiReviewCandidateResult(0, 1, 0, 0, 0);
    }

    public static AiReviewCandidateResult failed() {
        return new AiReviewCandidateResult(0, 0, 1, 0, 0);
    }

    public static AiReviewCandidateResult retryWaiting() {
        return new AiReviewCandidateResult(0, 0, 0, 1, 0);
    }

    public static AiReviewCandidateResult skipped() {
        return new AiReviewCandidateResult(0, 0, 0, 0, 0);
    }

    public AiReviewRunResult toRunResult() {
        return new AiReviewRunResult(
                1,
                approvedCount,
                rejectedCount,
                failedCount,
                retryWaitingCount,
                storedCount,
                0
        );
    }
}
