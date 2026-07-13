package com.globaltechblogarchive.slack.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.globaltechblogarchive.slack.application.SlackMessageClient;
import com.globaltechblogarchive.slack.application.SlackMessageSendResult;
import com.globaltechblogarchive.slack.application.digest.SlackChatMessage;
import com.globaltechblogarchive.slack.exception.SlackMessageSendException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SlackMessageRestClient implements SlackMessageClient {

    private static final String SLACK_API_BASE_URL = "https://slack.com";
    private static final String CHAT_POST_MESSAGE_PATH = "/api/chat.postMessage";
    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(500);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);
    private static final Set<String> RETRYABLE_SLACK_ERRORS = Set.of(
            "ratelimited",
            "internal_error",
            "fatal_error",
            "request_timeout",
            "service_unavailable"
    );

    private final RestClient restClient;

    @Autowired
    public SlackMessageRestClient(RestClient.Builder restClientBuilder) {
        this(createRestClient(restClientBuilder));
    }

    SlackMessageRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public SlackMessageSendResult send(String botToken, SlackChatMessage message) {
        SlackChatPostMessageResponse response;
        try {
            response = restClient.post()
                    .uri(CHAT_POST_MESSAGE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(botToken))
                    .body(message)
                    .retrieve()
                    .body(SlackChatPostMessageResponse.class);
        } catch (RestClientResponseException exception) {
            throw fromHttpError(exception);
        } catch (RestClientException exception) {
            throw new SlackMessageSendException(
                    "NETWORK_ERROR",
                    "Slack chat.postMessage 네트워크 요청에 실패했습니다.",
                    true,
                    null
            );
        }

        if (response == null) {
            throw new SlackMessageSendException(
                    "EMPTY_RESPONSE",
                    "Slack chat.postMessage 응답이 비어 있습니다.",
                    true,
                    null
            );
        }
        if (!response.ok()) {
            String errorCode = response.error();
            if (errorCode == null || errorCode.isBlank()) {
                errorCode = "UNKNOWN_SLACK_ERROR";
            }
            throw new SlackMessageSendException(
                    errorCode,
                    "Slack chat.postMessage 실패: " + errorCode,
                    RETRYABLE_SLACK_ERRORS.contains(errorCode),
                    null
            );
        }
        return new SlackMessageSendResult(response.ts());
    }

    private SlackMessageSendException fromHttpError(RestClientResponseException exception) {
        if (exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return new SlackMessageSendException(
                    "HTTP_429",
                    "Slack API 호출 한도를 초과했습니다.",
                    true,
                    retryAfter(exception.getResponseHeaders())
            );
        }
        if (exception.getStatusCode().is5xxServerError()) {
            return new SlackMessageSendException(
                    "HTTP_" + exception.getStatusCode().value(),
                    "Slack API 서버 오류가 발생했습니다.",
                    true,
                    null
            );
        }
        return new SlackMessageSendException(
                "HTTP_" + exception.getStatusCode().value(),
                "Slack API 요청이 거부되었습니다.",
                false,
                null
        );
    }

    private Duration retryAfter(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static RestClient createRestClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return builder
                .baseUrl(SLACK_API_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SlackChatPostMessageResponse(boolean ok, String error, String ts) {
    }
}
