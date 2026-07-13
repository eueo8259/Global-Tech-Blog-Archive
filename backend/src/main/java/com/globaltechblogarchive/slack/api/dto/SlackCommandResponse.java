package com.globaltechblogarchive.slack.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SlackCommandResponse(
        @JsonProperty("response_type") String responseType,
        String text
) {

    public static SlackCommandResponse ephemeral(String text) {
        return new SlackCommandResponse("ephemeral", text);
    }
}
