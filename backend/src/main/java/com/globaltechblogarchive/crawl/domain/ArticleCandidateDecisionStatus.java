package com.globaltechblogarchive.crawl.domain;

public enum ArticleCandidateDecisionStatus {
    NEW,
    PREVIOUSLY_APPROVED,
    PREVIOUSLY_REJECTED,
    AI_APPROVED,
    AI_REJECTED,
    AI_FAILED
}
