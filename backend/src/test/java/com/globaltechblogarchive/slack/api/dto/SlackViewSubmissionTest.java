package com.globaltechblogarchive.slack.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

class SlackViewSubmissionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromParsesSelectedCompaniesAndMetadata() throws Exception {
        String payload = payload("view_submission", "subscription_settings", "T123", "T123", "netflix", "uber");

        SlackViewSubmission submission = SlackViewSubmission.from(payload, objectMapper);

        assertThat(submission.metadata().teamId()).isEqualTo("T123");
        assertThat(submission.metadata().channelId()).isEqualTo("C123");
        assertThat(submission.metadata().channelName()).isEqualTo("tech-news");
        assertThat(submission.selectedCompanyKeys()).containsExactlyInAnyOrder("netflix", "uber");
    }

    @Test
    void fromAllowsEmptySelection() throws Exception {
        SlackViewSubmission submission = SlackViewSubmission.from(
                payload("view_submission", "subscription_settings", "T123", "T123"),
                objectMapper
        );

        assertThat(submission.selectedCompanyKeys()).isEmpty();
    }

    @Test
    void fromRemovesDuplicateCompanyKeys() throws Exception {
        SlackViewSubmission submission = SlackViewSubmission.from(
                payload("view_submission", "subscription_settings", "T123", "T123", "netflix", "netflix"),
                objectMapper
        );

        assertThat(submission.selectedCompanyKeys()).containsExactly("netflix");
    }

    @Test
    void fromRejectsMissingPayload() {
        assertThatThrownBy(() -> SlackViewSubmission.from(null, objectMapper))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void fromRejectsMalformedJson() {
        assertThatThrownBy(() -> SlackViewSubmission.from("{", objectMapper))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void fromRejectsUnsupportedType() throws Exception {
        String payload = payload("block_actions", "subscription_settings", "T123", "T123");

        assertThatThrownBy(() -> SlackViewSubmission.from(payload, objectMapper))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void fromRejectsUnsupportedCallbackId() throws Exception {
        String payload = payload("view_submission", "other", "T123", "T123");

        assertThatThrownBy(() -> SlackViewSubmission.from(payload, objectMapper))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void fromRejectsMismatchedTeam() throws Exception {
        String payload = payload("view_submission", "subscription_settings", "T999", "T123");

        assertThatThrownBy(() -> SlackViewSubmission.from(payload, objectMapper))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void fromRejectsMissingSelectionState() throws Exception {
        ObjectNode root = (ObjectNode) objectMapper.readTree(
                payload("view_submission", "subscription_settings", "T123", "T123")
        );
        ((ObjectNode) root.path("view")).remove("state");

        assertThatThrownBy(() -> SlackViewSubmission.from(root.toString(), objectMapper))
                .isInstanceOf(InvalidInputException.class);
    }

    private String payload(
            String type,
            String callbackId,
            String rootTeamId,
            String metadataTeamId,
            String... companyKeys
    ) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", type);
        root.putObject("team").put("id", rootTeamId);
        ObjectNode view = root.putObject("view");
        view.put("callback_id", callbackId);
        view.put("private_metadata", objectMapper.writeValueAsString(new TestMetadata(
                metadataTeamId,
                "C123",
                "tech-news"
        )));
        ArrayNode selectedOptions = view.putObject("state")
                .putObject("values")
                .putObject("company_subscriptions")
                .putObject("selected_companies")
                .putArray("selected_options");
        for (String companyKey : companyKeys) {
            selectedOptions.addObject().put("value", companyKey);
        }
        return root.toString();
    }

    private record TestMetadata(String teamId, String channelId, String channelName) {
    }
}
