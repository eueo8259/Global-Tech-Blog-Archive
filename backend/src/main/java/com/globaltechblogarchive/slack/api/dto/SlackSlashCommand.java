package com.globaltechblogarchive.slack.api.dto;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

public record SlackSlashCommand(
        String command,
        String teamId,
        String channelId,
        String channelName,
        String userId,
        String triggerId
) {

    private static final String SUBSCRIBE_COMMAND = "/subscribe";

    public static SlackSlashCommand from(MultiValueMap<String, String> payload) {
        if (payload == null) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Slack command payload is required");
        }
        SlackSlashCommand command = new SlackSlashCommand(
                payload.getFirst("command"),
                payload.getFirst("team_id"),
                payload.getFirst("channel_id"),
                payload.getFirst("channel_name"),
                payload.getFirst("user_id"),
                payload.getFirst("trigger_id")
        );
        command.validate();
        return command;
    }

    private void validate() {
        requireText(command, "command");
        requireText(teamId, "team_id");
        requireText(channelId, "channel_id");
        requireText(channelName, "channel_name");
        requireText(userId, "user_id");
        requireText(triggerId, "trigger_id");
        if (!SUBSCRIBE_COMMAND.equals(command)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Unsupported Slack command");
        }
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, fieldName + " is required");
        }
    }
}
