package com.globaltechblogarchive.slack.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.globaltechblogarchive.slack.application.modal.SlackSubscriptionModalContract;
import java.util.Map;

public record SlackInteractivityResponse(
        @JsonProperty("response_action") String responseAction,
        Map<String, String> errors
) {

    public static SlackInteractivityResponse companySelectionError(String message) {
        return new SlackInteractivityResponse(
                "errors",
                Map.of(SlackSubscriptionModalContract.COMPANY_BLOCK_ID, message)
        );
    }
}
