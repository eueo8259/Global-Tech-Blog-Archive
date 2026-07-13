package com.globaltechblogarchive.slack.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.slack.application.SlackViewClient;
import com.globaltechblogarchive.slack.application.modal.SlackModalView;
import com.globaltechblogarchive.slack.exception.SlackViewException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SlackViewRestClient implements SlackViewClient {

    private static final String SLACK_API_BASE_URL = "https://slack.com";
    private static final String VIEWS_OPEN_PATH = "/api/views.open";
    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(500);
    private static final Duration READ_TIMEOUT = Duration.ofMillis(1500);

    private final RestClient restClient;

    @Autowired
    public SlackViewRestClient(RestClient.Builder restClientBuilder) {
        this(createRestClient(restClientBuilder));
    }

    SlackViewRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void open(String botToken, String triggerId, SlackModalView view) {
        SlackViewOpenResponse response;
        try {
            response = restClient.post()
                    .uri(VIEWS_OPEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(botToken))
                    .body(new SlackViewOpenRequest(triggerId, view))
                    .retrieve()
                    .body(SlackViewOpenResponse.class);
        } catch (RestClientException exception) {
            throw new SlackViewException(
                    ErrorCode.SLACK_VIEW_OPEN_ERROR,
                    "Slack views.open request failed"
            );
        }

        if (response == null || !response.ok()) {
            String slackError = response == null ? "empty_response" : response.error();
            throw new SlackViewException(
                    ErrorCode.SLACK_VIEW_OPEN_ERROR,
                    "Slack views.open failed: " + slackError
            );
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

    record SlackViewOpenRequest(
            @JsonProperty("trigger_id") String triggerId,
            SlackModalView view
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SlackViewOpenResponse(boolean ok, String error) {
    }
}
