package com.globaltechblogarchive.slack.domain;

public enum SlackDeliveryStatus {
    PENDING,
    PROCESSING,
    RETRY_WAITING,
    SENT,
    SENT_UNCONFIRMED,
    FAILED
}
