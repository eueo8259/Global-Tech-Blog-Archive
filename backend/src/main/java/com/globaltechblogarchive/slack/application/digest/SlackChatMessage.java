package com.globaltechblogarchive.slack.application.digest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SlackChatMessage(
        String channel,
        String text,
        List<Block> blocks,
        Metadata metadata
) {

    public record Metadata(
            @JsonProperty("event_type") String eventType,
            @JsonProperty("event_payload") EventPayload eventPayload
    ) {

        public static Metadata digest(String deliveryKey) {
            return new Metadata("techport_digest_sent", new EventPayload(deliveryKey));
        }
    }

    public record EventPayload(@JsonProperty("delivery_key") String deliveryKey) {
    }

    public record Block(
            String type,
            @JsonInclude(JsonInclude.Include.NON_NULL) Text text,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<Text> elements
    ) {

        public static Block header(String text) {
            return new Block("header", new Text("plain_text", text), null);
        }

        public static Block section(String text) {
            return new Block("section", new Text("mrkdwn", text), null);
        }

        public static Block context(String text) {
            return new Block("context", null, List.of(new Text("mrkdwn", text)));
        }
    }

    public record Text(String type, String text) {
    }
}
