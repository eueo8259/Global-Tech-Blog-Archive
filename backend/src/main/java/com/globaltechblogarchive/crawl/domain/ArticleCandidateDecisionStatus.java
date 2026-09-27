package com.globaltechblogarchive.crawl.domain;

public enum ArticleCandidateDecisionStatus {
    NEW,
    AI_PROCESSING,
    AI_RETRY_WAITING,
    PREVIOUSLY_APPROVED,
    PREVIOUSLY_REJECTED,
    AI_APPROVED,
    AI_REJECTED,
    AI_FAILED,
    DUPLICATE
}
