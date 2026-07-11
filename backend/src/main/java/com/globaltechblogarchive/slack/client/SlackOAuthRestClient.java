package com.globaltechblogarchive.slack.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.slack.application.SlackOAuthClient;
import com.globaltechblogarchive.slack.config.SlackProperties;
import com.globaltechblogarchive.slack.exception.SlackOAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class SlackOAuthRestClient implements SlackOAuthClient {

    private static final String SLACK_API_BASE_URL = "https://slack.com";
    private static final String OAUTH_ACCESS_PATH = "/api/oauth.v2.access";

    private final RestClient.Builder restClientBuilder;
    private final SlackProperties properties;

    @Override
    public SlackOAuthInstallation exchangeCode(String code) {
        SlackOAuthAccessResponse response;
        try {
            response = restClientBuilder.baseUrl(SLACK_API_BASE_URL)
                    .build()
                    .post()
                    .uri(OAUTH_ACCESS_PATH)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createAccessRequest(code))
                    .retrieve()
                    .body(SlackOAuthAccessResponse.class);
        } catch (RestClientException exception) {
            throw new SlackOAuthException(ErrorCode.SLACK_OAUTH_ERROR, "Slack OAuth token exchange failed");
        }

        if (response == null || !response.ok()) {
            String slackError = response == null ? "empty_response" : response.error();
            throw new SlackOAuthException(ErrorCode.SLACK_OAUTH_ERROR, "Slack OAuth failed: " + slackError);
        }
        if (!StringUtils.hasText(response.accessToken())
                || response.team() == null
                || !StringUtils.hasText(response.team().id())) {
            throw new SlackOAuthException(ErrorCode.SLACK_OAUTH_ERROR, "Slack OAuth response is missing installation data");
        }

        return new SlackOAuthInstallation(
                response.team().id(),
                response.team().name(),
                response.accessToken(),
                response.botUserId(),
                response.scope()
        );
    }

    private LinkedMultiValueMap<String, String> createAccessRequest(String code) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("code", code);
        body.add("redirect_uri", properties.redirectUri());
        return body;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SlackOAuthAccessResponse(
            boolean ok,
            String error,
            String scope,
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("bot_user_id") String botUserId,
            SlackOAuthTeamResponse team
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SlackOAuthTeamResponse(
            String id,
            String name
    ) {
    }
}
