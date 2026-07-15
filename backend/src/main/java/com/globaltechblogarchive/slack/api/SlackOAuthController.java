package com.globaltechblogarchive.slack.api;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.slack.application.SlackOAuthService;
import com.globaltechblogarchive.slack.config.SlackProperties;
import com.globaltechblogarchive.slack.support.SlackOAuthStateStore;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackOAuthController {

    private final SlackOAuthService slackOAuthService;
    private final SlackOAuthStateStore stateStore;
    private final SlackProperties properties;

    @GetMapping("/api/slack/oauth/authorize")
    public RedirectView authorize(HttpSession session) {
        String state = stateStore.createState(session);
        return new RedirectView(slackOAuthService.createAuthorizeUrl(state));
    }

    @GetMapping("/api/slack/oauth/callback")
    public RedirectView callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session
    ) {
        if (StringUtils.hasText(error)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Slack OAuth authorization was cancelled");
        }
        if (!StringUtils.hasText(code)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Slack OAuth code is required");
        }
        if (!stateStore.validateAndConsume(session, state)) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Slack OAuth state is invalid");
        }

        slackOAuthService.install(code);
        String successUrl = UriComponentsBuilder.fromUriString(properties.settingsBaseUrl())
                .pathSegment("slack", "success")
                .build()
                .toUriString();
        return new RedirectView(successUrl);
    }
}
