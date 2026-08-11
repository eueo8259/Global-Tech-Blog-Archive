package com.globaltechblogarchive.slack.application.digest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.application.SlackMessageLookupClient;
import com.globaltechblogarchive.slack.application.SlackMessageLookupResult;
import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import com.globaltechblogarchive.slack.exception.SlackMessageLookupException;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlackDeliveryVerificationServiceTest {

    private final SlackDeliveryStateService stateService = mock(SlackDeliveryStateService.class);
    private final SlackMessageLookupClient lookupClient = mock(SlackMessageLookupClient.class);
    private final SlackTokenEncryptor tokenEncryptor = mock(SlackTokenEncryptor.class);
    private final SlackDeliveryVerificationService service = new SlackDeliveryVerificationService(
            stateService,
            lookupClient,
            tokenEncryptor,
            properties()
    );

    private LocalDateTime now;
    private VerifyingSlackDelivery delivery;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 8, 11, 9, 5);
        delivery = new VerifyingSlackDelivery(
                1L,
                "C123",
                "encrypted-token",
                "delivery-key",
                now.minusMinutes(5),
                null
        );
        when(stateService.findVerificationTarget(1L, now)).thenReturn(Optional.of(delivery));
        when(tokenEncryptor.decrypt("encrypted-token")).thenReturn("xoxb-token");
    }

    @Test
    void verifyMarksSentWithoutResendingWhenMessageExists() {
        when(lookupClient.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                now.minusMinutes(5),
                now,
                null
        )).thenReturn(Optional.of(new SlackMessageLookupResult("1720937160.000100")));

        service.verify(1L, now);

        verify(stateService).markSent(1L, now, "1720937160.000100");
        verify(stateService, never()).recordVerificationNotFound(1L, now.plusMinutes(5));
    }

    @Test
    void verifyRecordsOnlySuccessfulNotFoundLookup() {
        when(lookupClient.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                now.minusMinutes(5),
                now,
                null
        )).thenReturn(Optional.empty());

        service.verify(1L, now);

        verify(stateService).recordVerificationNotFound(1L, now.plusMinutes(5));
    }

    @Test
    void verifyUsesRetryAfterWithoutRecordingNotFound() {
        when(lookupClient.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                now.minusMinutes(5),
                now,
                null
        )).thenThrow(new SlackMessageLookupException(
                "HTTP_429",
                "rate limited",
                true,
                Duration.ofSeconds(30)
        ));

        service.verify(1L, now);

        verify(stateService).scheduleNextVerification(
                1L,
                now.plusSeconds(30),
                "HTTP_429",
                "rate limited"
        );
        verify(stateService, never()).recordVerificationNotFound(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void verifyUsesLongDelayForPermissionError() {
        when(lookupClient.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                now.minusMinutes(5),
                now,
                null
        )).thenThrow(new SlackMessageLookupException(
                "missing_scope",
                "scope missing",
                false,
                null
        ));

        service.verify(1L, now);

        verify(stateService).scheduleNextVerification(
                1L,
                now.plusHours(1),
                "missing_scope",
                "scope missing"
        );
    }

    @Test
    void verifyUsesLongDelayForExpiredOrRevokedAuthentication() {
        when(lookupClient.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                now.minusMinutes(5),
                now,
                null
        )).thenThrow(new SlackMessageLookupException(
                "token_revoked",
                "token revoked",
                false,
                null
        ));

        service.verify(1L, now);

        verify(stateService).scheduleNextVerification(
                1L,
                now.plusHours(1),
                "token_revoked",
                "token revoked"
        );
    }

    private SlackDailyDigestProperties properties() {
        return new SlackDailyDigestProperties(
                "Asia/Seoul",
                LocalTime.of(9, 0),
                3,
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                Duration.ofMinutes(5),
                Duration.ofHours(1),
                3,
                Duration.ofMinutes(1),
                15
        );
    }
}
