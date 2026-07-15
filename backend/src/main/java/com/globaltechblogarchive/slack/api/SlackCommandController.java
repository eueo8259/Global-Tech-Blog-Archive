package com.globaltechblogarchive.slack.api;

import com.globaltechblogarchive.slack.api.dto.SlackCommandResponse;
import com.globaltechblogarchive.slack.api.dto.SlackSlashCommand;
import com.globaltechblogarchive.slack.application.SlackCommandResult;
import com.globaltechblogarchive.slack.application.SlackCommandService;
import com.globaltechblogarchive.slack.filter.SlackRequestSignatureFilter;
import com.globaltechblogarchive.slack.support.SlackFormPayloadParser;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackCommandController {

    private final SlackCommandService slackCommandService;
    private final SlackFormPayloadParser formPayloadParser;

    @PostMapping(
            value = "/api/slack/commands",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<SlackCommandResponse> command(
            @RequestAttribute(name = SlackRequestSignatureFilter.RAW_BODY_ATTRIBUTE) byte[] rawBody
    ) {
        SlackSlashCommand command = SlackSlashCommand.from(formPayloadParser.parse(rawBody));
        SlackCommandResult result = slackCommandService.openSubscriptionModal(command);
        if (result.succeeded()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(SlackCommandResponse.ephemeral(result.userMessage()));
    }
}
