package com.globaltechblogarchive.slack.application.digest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.application.SlackMessageClient;
import com.globaltechblogarchive.slack.application.SlackMessageSendResult;
import com.globaltechblogarchive.slack.exception.SlackMessageSendException;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlackDeliveryDispatchServiceTest {

    private final SlackDeliveryStateService stateService = mock(SlackDeliveryStateService.class);
    private final SlackDigestQueryService queryService = mock(SlackDigestQueryService.class);
    private final SlackDigestMessageFactory messageFactory = mock(SlackDigestMessageFactory.class);
    private final SlackMessageClient messageClient = mock(SlackMessageClient.class);
    private final SlackTokenEncryptor tokenEncryptor = mock(SlackTokenEncryptor.class);
    private final SlackDeliveryDispatchService service = new SlackDeliveryDispatchService(
            stateService,
            queryService,
            messageFactory,
            messageClient,
            tokenEncryptor
    );

    private LocalDateTime now;
    private ClaimedSlackDelivery claimed;
    private List<SlackDigestArticle> articles;
    private SlackChatMessage message;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 7, 14, 9, 0);
        claimed = new ClaimedSlackDelivery(
                1L,
                10L,
                "C123",
                "encrypted-token",
                now.minusDays(1),
                now,
                1
        );
        articles = List.of(new SlackDigestArticle(
                100L,
                20L,
                "Netflix",
                "Scaling Java",
                "https://example.com/scaling-java",
                now.minusHours(2)
        ));
        message = new SlackChatMessage("C123", "digest", List.of());
        when(stateService.claim(1L, now)).thenReturn(Optional.of(claimed));
        when(queryService.findArticles(10L, now.minusDays(1), now)).thenReturn(articles);
        when(messageFactory.create("C123", articles)).thenReturn(message);
        when(tokenEncryptor.decrypt("encrypted-token")).thenReturn("xoxb-token");
    }

    @Test
    void dispatchSendsOneMessageAndMarksDeliverySent() {
        when(messageClient.send("xoxb-token", message))
                .thenReturn(new SlackMessageSendResult("1720937160.000100"));

        service.dispatch(1L, now);

        verify(messageClient).send("xoxb-token", message);
        verify(stateService).markSent(1L, now, "1720937160.000100");
    }

    @Test
    void dispatchSchedulesRetryForRateLimit() {
        SlackMessageSendException exception = new SlackMessageSendException(
                "HTTP_429",
                "rate limited",
                true,
                Duration.ofSeconds(30)
        );
        when(messageClient.send("xoxb-token", message)).thenThrow(exception);

        service.dispatch(1L, now);

        verify(stateService).markFailure(
                1L,
                now,
                true,
                Duration.ofSeconds(30),
                "HTTP_429",
                "rate limited"
        );
    }

    @Test
    void dispatchMarksPermanentSlackErrorFailed() {
        SlackMessageSendException exception = new SlackMessageSendException(
                "channel_not_found",
                "channel missing",
                false,
                null
        );
        when(messageClient.send("xoxb-token", message)).thenThrow(exception);

        service.dispatch(1L, now);

        verify(stateService).markFailure(
                1L,
                now,
                false,
                null,
                "channel_not_found",
                "channel missing"
        );
    }

    @Test
    void dispatchCompletesWithoutSlackCallWhenRetryContentBecomesEmpty() {
        when(queryService.findArticles(10L, now.minusDays(1), now)).thenReturn(List.of());

        service.dispatch(1L, now);

        verify(messageClient, never()).send(any(), any());
        verify(stateService).markSent(1L, now, null);
    }

    @Test
    void dispatchSkipsAlreadyClaimedOrSentDelivery() {
        when(stateService.claim(1L, now)).thenReturn(Optional.empty());

        service.dispatch(1L, now);

        verify(queryService, never()).findArticles(any(), any(), any());
        verify(messageClient, never()).send(any(), any());
        verify(stateService, never()).markSent(eq(1L), any(), any());
    }
}
