package com.globaltechblogarchive.slack.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.application.SlackOAuthClient.SlackOAuthInstallation;
import com.globaltechblogarchive.slack.config.SlackProperties;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackWorkspaceRepository;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SlackOAuthServiceTest {

    private final SlackOAuthClient slackOAuthClient = mock(SlackOAuthClient.class);
    private final SlackWorkspaceRepository workspaceRepository = mock(SlackWorkspaceRepository.class);
    private final SlackTokenEncryptor slackTokenEncryptor = mock(SlackTokenEncryptor.class);
    private final SlackOAuthService service = new SlackOAuthService(
            new SlackProperties(
                    "client-id",
                    "client-secret",
                    "signing-secret",
                    "http://localhost:8080/api/slack/oauth/callback",
                    "commands,chat:write",
                    "http://localhost:5173"
            ),
            slackOAuthClient,
            workspaceRepository,
            slackTokenEncryptor
    );

    @Test
    void createAuthorizeUrlIncludesSlackOAuthParameters() {
        String authorizeUrl = service.createAuthorizeUrl("state-123");

        assertThat(authorizeUrl)
                .startsWith("https://slack.com/oauth/v2/authorize")
                .contains("client_id=client-id")
                .contains("scope=commands,chat:write")
                .contains("redirect_uri=http://localhost:8080/api/slack/oauth/callback")
                .contains("state=state-123");
    }

    @Test
    void installCreatesWorkspaceWhenTeamIsNew() {
        SlackOAuthInstallation installation = installation("T123", "TechPort", "xoxb-token", "B123", "commands");
        SlackWorkspace savedWorkspace = SlackWorkspace.create(
                "T123",
                "TechPort",
                "encrypted-token",
                "B123",
                "commands",
                LocalDateTime.of(2026, 7, 9, 10, 0)
        );
        when(slackOAuthClient.exchangeCode("code")).thenReturn(installation);
        when(slackTokenEncryptor.encrypt("xoxb-token")).thenReturn("encrypted-token");
        when(workspaceRepository.findBySlackTeamId("T123")).thenReturn(Optional.empty());
        when(workspaceRepository.save(org.mockito.ArgumentMatchers.any(SlackWorkspace.class))).thenReturn(savedWorkspace);

        SlackWorkspace workspace = service.install("code");

        assertThat(workspace.getSlackTeamId()).isEqualTo("T123");
        assertThat(workspace.getEncryptedBotToken()).isEqualTo("encrypted-token");
        verify(workspaceRepository).save(org.mockito.ArgumentMatchers.any(SlackWorkspace.class));
    }

    @Test
    void installUpdatesWorkspaceWhenTeamAlreadyExists() {
        SlackWorkspace workspace = SlackWorkspace.create(
                "T123",
                "Old Name",
                "old-token",
                "B111",
                "commands",
                LocalDateTime.of(2026, 7, 9, 10, 0)
        );
        when(slackOAuthClient.exchangeCode("code"))
                .thenReturn(installation("T123", "New Name", "xoxb-token", "B222", "commands,chat:write"));
        when(slackTokenEncryptor.encrypt("xoxb-token")).thenReturn("new-encrypted-token");
        when(workspaceRepository.findBySlackTeamId("T123")).thenReturn(Optional.of(workspace));

        SlackWorkspace updatedWorkspace = service.install("code");

        assertThat(updatedWorkspace.getSlackTeamName()).isEqualTo("New Name");
        assertThat(updatedWorkspace.getEncryptedBotToken()).isEqualTo("new-encrypted-token");
        assertThat(updatedWorkspace.getBotUserId()).isEqualTo("B222");
        assertThat(updatedWorkspace.getScope()).isEqualTo("commands,chat:write");
        verify(workspaceRepository, never()).save(org.mockito.ArgumentMatchers.any(SlackWorkspace.class));
    }

    private SlackOAuthInstallation installation(
            String teamId,
            String teamName,
            String accessToken,
            String botUserId,
            String scope
    ) {
        return new SlackOAuthInstallation(teamId, teamName, accessToken, botUserId, scope);
    }
}
