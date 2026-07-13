package com.globaltechblogarchive.slack.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.api.dto.SlackSlashCommand;
import com.globaltechblogarchive.slack.application.SlackSubscriptionModalData.CompanyOption;
import com.globaltechblogarchive.slack.application.modal.SlackModalMetadata;
import com.globaltechblogarchive.slack.application.modal.SlackModalView;
import com.globaltechblogarchive.slack.application.modal.SlackSubscriptionModalFactory;
import com.globaltechblogarchive.slack.exception.SlackViewException;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SlackCommandServiceTest {

    private final SlackSubscriptionQueryService queryService = mock(SlackSubscriptionQueryService.class);
    private final SlackTokenEncryptor tokenEncryptor = mock(SlackTokenEncryptor.class);
    private final SlackSubscriptionModalFactory modalFactory = mock(SlackSubscriptionModalFactory.class);
    private final SlackViewClient slackViewClient = mock(SlackViewClient.class);
    private final SlackCommandService service = new SlackCommandService(
            queryService,
            tokenEncryptor,
            modalFactory,
            slackViewClient
    );

    @Test
    void openSubscriptionModalDecryptsTokenAndOpensView() {
        SlackSlashCommand command = command();
        SlackSubscriptionModalData data = data();
        SlackModalView view = mock(SlackModalView.class);
        when(queryService.findModalData("T123", "C123")).thenReturn(Optional.of(data));
        when(tokenEncryptor.decrypt("encrypted-token")).thenReturn("xoxb-token");
        when(modalFactory.create(metadata(), data.companies())).thenReturn(view);

        SlackCommandResult result = service.openSubscriptionModal(command);

        assertThat(result.succeeded()).isTrue();
        verify(slackViewClient).open("xoxb-token", "trigger-123", view);
    }

    @Test
    void openSubscriptionModalReturnsUserMessageWhenWorkspaceDoesNotExist() {
        when(queryService.findModalData("T123", "C123")).thenReturn(Optional.empty());

        SlackCommandResult result = service.openSubscriptionModal(command());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.userMessage()).contains("앱 설치 정보");
        verify(slackViewClient, never()).open(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void openSubscriptionModalReturnsUserMessageWhenSlackRequestFails() {
        SlackSlashCommand command = command();
        SlackSubscriptionModalData data = data();
        SlackModalView view = mock(SlackModalView.class);
        when(queryService.findModalData("T123", "C123")).thenReturn(Optional.of(data));
        when(tokenEncryptor.decrypt("encrypted-token")).thenReturn("xoxb-token");
        when(modalFactory.create(metadata(), data.companies())).thenReturn(view);
        org.mockito.Mockito.doThrow(new SlackViewException(
                        com.globaltechblogarchive.global.error.ErrorCode.SLACK_VIEW_OPEN_ERROR,
                        "expired_trigger_id"
                ))
                .when(slackViewClient).open("xoxb-token", "trigger-123", view);

        SlackCommandResult result = service.openSubscriptionModal(command);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.userMessage()).contains("구독 설정 화면");
    }

    private SlackSlashCommand command() {
        return new SlackSlashCommand(
                "/subscribe", "T123", "C123", "tech-news", "U123", "trigger-123"
        );
    }

    private SlackSubscriptionModalData data() {
        return new SlackSubscriptionModalData(
                "encrypted-token",
                List.of(new CompanyOption("netflix", "Netflix", true))
        );
    }

    private SlackModalMetadata metadata() {
        return new SlackModalMetadata("T123", "C123", "tech-news");
    }
}
