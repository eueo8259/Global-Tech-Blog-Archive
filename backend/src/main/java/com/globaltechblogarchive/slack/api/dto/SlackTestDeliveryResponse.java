package com.globaltechblogarchive.slack.api.dto;

public record SlackTestDeliveryResponse(
        String companyKey,
        int sentChannelCount
) {
}
