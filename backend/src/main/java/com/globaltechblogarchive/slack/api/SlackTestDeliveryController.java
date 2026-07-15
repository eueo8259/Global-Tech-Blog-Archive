package com.globaltechblogarchive.slack.api;

import com.globaltechblogarchive.slack.api.dto.SlackTestDeliveryRequest;
import com.globaltechblogarchive.slack.api.dto.SlackTestDeliveryResponse;
import com.globaltechblogarchive.slack.application.SlackTestDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackTestDeliveryController {

    private final SlackTestDeliveryService testDeliveryService;

    @PostMapping("/api/admin/slack/test-deliveries")
    public ResponseEntity<SlackTestDeliveryResponse> send(
            @RequestBody SlackTestDeliveryRequest request
    ) {
        request.validate();
        int sentChannelCount = testDeliveryService.send(
                request.companyKey(),
                request.title(),
                request.articleUrl()
        );
        return ResponseEntity.ok(new SlackTestDeliveryResponse(
                request.companyKey(),
                sentChannelCount
        ));
    }
}
