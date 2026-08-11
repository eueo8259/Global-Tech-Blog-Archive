package com.globaltechblogarchive.slack.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.globaltechblogarchive.slack.application.SlackMessageLookupClient;
import com.globaltechblogarchive.slack.application.SlackMessageLookupResult;
import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import com.globaltechblogarchive.slack.exception.SlackMessageLookupException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SlackHistoryRestClient implements SlackMessageLookupClient {

    private static final String SLACK_API_BASE_URL = "https://slack.com";
    private static final String CONVERSATIONS_HISTORY_PATH = "/api/conversations.history";
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
    private final SlackDailyDigestProperties properties;
    private final ZoneId zoneId;

    @Autowired
    public SlackHistoryRestClient(
            RestClient.Builder restClientBuilder,
            SlackDailyDigestProperties properties
    ) {
        this(createRestClient(restClientBuilder), properties);
    }

    SlackHistoryRestClient(RestClient restClient, SlackDailyDigestProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
        this.zoneId = ZoneId.of(properties.zone());
    }

    @Override
    public Optional<SlackMessageLookupResult> findByDeliveryKey(
            String botToken,
            String channelId,
            String deliveryKey,
            LocalDateTime attemptedAt,
            LocalDateTime verificationNow,
            String knownMessageTs
    ) {
        if (knownMessageTs != null && !knownMessageTs.isBlank()) {
            HistoryResponse exactResponse = requestPage(
                    botToken,
                    channelId,
                    knownMessageTs,
                    knownMessageTs,
                    1,
                    null
            );
            Optional<SlackMessageLookupResult> exactMatch = messages(exactResponse).stream()
                    .filter(message -> knownMessageTs.equals(message.ts()))
                    .map(message -> new SlackMessageLookupResult(message.ts()))
                    .findFirst();
            if (exactMatch.isPresent()) {
                return exactMatch;
            }
        }

        if (attemptedAt == null) {
            throw new SlackMessageLookupException(
                    "MISSING_ATTEMPTED_AT",
                    "Slack 메시지 조회 기준 시각이 없습니다.",
                    false,
                    null
            );
        }

        String oldest = toSlackTimestamp(attemptedAt.minus(properties.historyLookback()));
        String latest = toSlackTimestamp(verificationNow);
        String cursor = null;
        while (true) {
            HistoryResponse response = requestPage(
                    botToken,
                    channelId,
                    oldest,
                    latest,
                    properties.historyPageSize(),
                    cursor
            );
            Optional<SlackMessageLookupResult> match = messages(response).stream()
                    .filter(message -> hasDeliveryMetadata(message, deliveryKey))
                    .map(message -> new SlackMessageLookupResult(message.ts()))
                    .findFirst();
            if (match.isPresent()) {
                return match;
            }
            if (!response.hasMore()) {
                return Optional.empty();
            }
            cursor = nextCursor(response);
            if (cursor == null || cursor.isBlank()) {
                throw new SlackMessageLookupException(
                        "INVALID_PAGINATION_RESPONSE",
                        "Slack conversations.history 다음 페이지 cursor가 없습니다.",
                        true,
                        null
                );
            }
        }
    }

    private HistoryResponse requestPage(
            String botToken,
            String channelId,
            String oldest,
            String latest,
            int limit,
            String cursor
    ) {
        HistoryResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(CONVERSATIONS_HISTORY_PATH)
                                .queryParam("channel", channelId)
                                .queryParam("include_all_metadata", true)
                                .queryParam("oldest", oldest)
                                .queryParam("latest", latest)
                                .queryParam("inclusive", true)
                                .queryParam("limit", limit);
                        if (cursor != null && !cursor.isBlank()) {
                            uriBuilder.queryParam("cursor", cursor);
                        }
                        return uriBuilder.build();
                    })
                    .headers(headers -> headers.setBearerAuth(botToken))
                    .retrieve()
                    .body(HistoryResponse.class);
        } catch (RestClientResponseException exception) {
            throw fromHttpError(exception);
        } catch (RestClientException exception) {
            throw new SlackMessageLookupException(
                    "NETWORK_ERROR",
                    "Slack conversations.history 네트워크 요청에 실패했습니다.",
                    true,
                    null
            );
        }

        if (response == null) {
            throw new SlackMessageLookupException(
                    "EMPTY_RESPONSE",
                    "Slack conversations.history 응답이 비어 있습니다.",
                    true,
                    null
            );
        }
        if (!response.ok()) {
            String errorCode = response.error();
            if (errorCode == null || errorCode.isBlank()) {
                errorCode = "UNKNOWN_SLACK_ERROR";
            }
            throw new SlackMessageLookupException(
                    errorCode,
                    "Slack conversations.history 실패: " + errorCode,
                    RETRYABLE_SLACK_ERRORS.contains(errorCode),
                    null
            );
        }
        return response;
    }

    private boolean hasDeliveryMetadata(HistoryMessage message, String deliveryKey) {
        MessageMetadata metadata = message.metadata();
        return metadata != null
                && "techport_digest_sent".equals(metadata.eventType())
                && metadata.eventPayload() != null
                && deliveryKey.equals(metadata.eventPayload().deliveryKey());
    }

    private List<HistoryMessage> messages(HistoryResponse response) {
        if (response.messages() == null) {
            return List.of();
        }
        return response.messages();
    }

    private String nextCursor(HistoryResponse response) {
        if (response.responseMetadata() == null) {
            return null;
        }
        return response.responseMetadata().nextCursor();
    }

    private String toSlackTimestamp(LocalDateTime value) {
        Instant instant = value.atZone(zoneId).toInstant();
        return instant.getEpochSecond() + "." + String.format("%09d", instant.getNano());
    }

    private SlackMessageLookupException fromHttpError(RestClientResponseException exception) {
        if (exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return new SlackMessageLookupException(
                    "HTTP_429",
                    "Slack API 호출 한도를 초과했습니다.",
                    true,
                    retryAfter(exception.getResponseHeaders())
            );
        }
        if (exception.getStatusCode().is5xxServerError()) {
            return new SlackMessageLookupException(
                    "HTTP_" + exception.getStatusCode().value(),
                    "Slack API 서버 오류가 발생했습니다.",
                    true,
                    null
            );
        }
        return new SlackMessageLookupException(
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
    record HistoryResponse(
            boolean ok,
            String error,
            List<HistoryMessage> messages,
            @JsonProperty("has_more") boolean hasMore,
            @JsonProperty("response_metadata") ResponseMetadata responseMetadata
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HistoryMessage(String ts, MessageMetadata metadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageMetadata(
            @JsonProperty("event_type") String eventType,
            @JsonProperty("event_payload") EventPayload eventPayload
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EventPayload(@JsonProperty("delivery_key") String deliveryKey) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponseMetadata(@JsonProperty("next_cursor") String nextCursor) {
    }
}
