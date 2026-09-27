package com.globaltechblogarchive.slack.application.digest;

public record SlackDeliveryRetryResult(int recoveredCount, int readyCount) {
}
