package com.globaltechblogarchive.slack.application.digest;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record SlackChatMessage(
        String channel,
        String text,
        List<Block> blocks
) {

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
