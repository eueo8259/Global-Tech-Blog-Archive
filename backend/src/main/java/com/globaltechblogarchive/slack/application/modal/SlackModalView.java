package com.globaltechblogarchive.slack.application.modal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SlackModalView(
        String type,
        @JsonProperty("callback_id") String callbackId,
        PlainText title,
        PlainText submit,
        PlainText close,
        @JsonProperty("private_metadata") String privateMetadata,
        List<InputBlock> blocks
) {

    public record PlainText(String type, String text) {
    }

    public record InputBlock(
            String type,
            @JsonProperty("block_id") String blockId,
            boolean optional,
            PlainText label,
            MultiStaticSelect element
    ) {
    }

    public record MultiStaticSelect(
            String type,
            @JsonProperty("action_id") String actionId,
            PlainText placeholder,
            List<Option> options,
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            @JsonProperty("initial_options") List<Option> initialOptions
    ) {
    }

    public record Option(PlainText text, String value) {
    }
}
