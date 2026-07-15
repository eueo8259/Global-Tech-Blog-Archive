package com.globaltechblogarchive.slack.api.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.slack.application.modal.SlackModalMetadata;
import com.globaltechblogarchive.slack.application.modal.SlackSubscriptionModalContract;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.util.StringUtils;

public record SlackViewSubmission(
        SlackModalMetadata metadata,
        Set<String> selectedCompanyKeys
) {

    private static final String VIEW_SUBMISSION_TYPE = "view_submission";

    public SlackViewSubmission {
        selectedCompanyKeys = Set.copyOf(selectedCompanyKeys);
    }

    public static SlackViewSubmission from(String payload, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(payload)) {
            throw invalid("Slack interactivity payload is required");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            requireValue(root.path("type"), VIEW_SUBMISSION_TYPE, "Unsupported Slack interaction type");

            JsonNode view = requireObject(root.path("view"), "view is required");
            requireValue(
                    view.path("callback_id"),
                    SlackSubscriptionModalContract.CALLBACK_ID,
                    "Unsupported Slack modal callback"
            );

            String privateMetadata = requireText(view.path("private_metadata"), "private_metadata is required");
            SlackModalMetadata metadata = objectMapper.readValue(privateMetadata, SlackModalMetadata.class);
            validateMetadata(metadata);

            String teamId = requireText(root.path("team").path("id"), "team.id is required");
            if (!teamId.equals(metadata.teamId())) {
                throw invalid("Slack team does not match modal metadata");
            }

            Set<String> selectedCompanyKeys = selectedCompanyKeys(view);
            return new SlackViewSubmission(metadata, selectedCompanyKeys);
        } catch (JsonProcessingException exception) {
            throw invalid("Invalid Slack interactivity payload");
        }
    }

    private static Set<String> selectedCompanyKeys(JsonNode view) {
        JsonNode selectedOptions = view.path("state")
                .path("values")
                .path(SlackSubscriptionModalContract.COMPANY_BLOCK_ID)
                .path(SlackSubscriptionModalContract.COMPANY_ACTION_ID)
                .path("selected_options");
        if (!selectedOptions.isArray()) {
            throw invalid("Slack company selection state is required");
        }

        Set<String> companyKeys = new LinkedHashSet<>();
        for (JsonNode option : selectedOptions) {
            companyKeys.add(requireText(option.path("value"), "Selected company value is required"));
        }
        return companyKeys;
    }

    private static void validateMetadata(SlackModalMetadata metadata) {
        if (metadata == null
                || !StringUtils.hasText(metadata.teamId())
                || !StringUtils.hasText(metadata.channelId())
                || !StringUtils.hasText(metadata.channelName())) {
            throw invalid("Invalid Slack modal metadata");
        }
    }

    private static JsonNode requireObject(JsonNode node, String message) {
        if (!node.isObject()) {
            throw invalid(message);
        }
        return node;
    }

    private static String requireText(JsonNode node, String message) {
        if (!node.isTextual() || !StringUtils.hasText(node.textValue())) {
            throw invalid(message);
        }
        return node.textValue();
    }

    private static void requireValue(JsonNode node, String expected, String message) {
        if (!expected.equals(requireText(node, message))) {
            throw invalid(message);
        }
    }

    private static InvalidInputException invalid(String message) {
        return new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, message);
    }
}
