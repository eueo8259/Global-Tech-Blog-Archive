package com.globaltechblogarchive.slack.domain;

public enum SlackDeliveryStatus {
    PENDING,
    PROCESSING,
    VERIFYING,
    RETRY_WAITING,
    SENT,
    FAILED
}
