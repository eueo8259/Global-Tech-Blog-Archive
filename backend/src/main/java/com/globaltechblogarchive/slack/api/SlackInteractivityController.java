package com.globaltechblogarchive.slack.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.slack.api.dto.SlackInteractivityResponse;
import com.globaltechblogarchive.slack.api.dto.SlackViewSubmission;
import com.globaltechblogarchive.slack.application.SlackSubscriptionCommandService;
import com.globaltechblogarchive.slack.exception.InvalidSlackCompanySelectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackInteractivityController {

    private static final String INVALID_COMPANY_MESSAGE = "존재하지 않는 회사가 포함되어 있습니다. 다시 시도해 주세요.";
    private static final String RETRY_MESSAGE = "잠시 후 다시 시도해 주세요.";

    private final SlackSubscriptionCommandService subscriptionCommandService;
    private final ObjectMapper objectMapper;

    @PostMapping(
            value = "/api/slack/interactivity",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<SlackInteractivityResponse> interactivity(
            @RequestBody(required = false) MultiValueMap<String, String> form
    ) {
        String payload = null;
        if (form != null) {
            payload = form.getFirst("payload");
        }

        // 파싱 실패는 그대로 전파되어 HTTP 400으로 처리된다. 서비스 단계의 회사 선택 실패만
        // Slack 필드 에러(response_action: errors)로 변환한다.
        SlackViewSubmission submission = SlackViewSubmission.from(payload, objectMapper);
        try {
            subscriptionCommandService.updateSubscriptions(submission);
            return ResponseEntity.ok().build();
        } catch (InvalidSlackCompanySelectionException exception) {
            return ResponseEntity.ok(
                    SlackInteractivityResponse.companySelectionError(INVALID_COMPANY_MESSAGE)
            );
        } catch (RuntimeException exception) {
            // Slack은 반드시 응답(ack)을 요구하므로, 예상치 못한 실패도 재시도 가능한
            // 필드 에러로 응답한다.
            log.warn(
                    "Unexpected Slack subscription update failure: teamId={}, channelId={}",
                    submission.metadata().teamId(),
                    submission.metadata().channelId(),
                    exception
            );
            return ResponseEntity.ok(SlackInteractivityResponse.companySelectionError(RETRY_MESSAGE));
        }
    }
}
