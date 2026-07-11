package com.globaltechblogarchive.slack.application;

import com.globaltechblogarchive.slack.application.SlackOAuthClient.SlackOAuthInstallation;
import com.globaltechblogarchive.slack.config.SlackProperties;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackWorkspaceRepository;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackOAuthService {

    private static final String SLACK_AUTHORIZE_URL = "https://slack.com/oauth/v2/authorize";

    private final SlackProperties properties;
    private final SlackOAuthClient slackOAuthClient;
    private final SlackWorkspaceRepository workspaceRepository;
    private final SlackTokenEncryptor slackTokenEncryptor;

    public String createAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(SLACK_AUTHORIZE_URL)
                .queryParam("client_id", properties.clientId())
                .queryParam("scope", properties.botScopes())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Transactional
    public SlackWorkspace install(String code) {
        SlackOAuthInstallation installation = slackOAuthClient.exchangeCode(code);
        String encryptedBotToken = slackTokenEncryptor.encrypt(installation.accessToken());
        LocalDateTime installedAt = LocalDateTime.now();

        return workspaceRepository.findBySlackTeamId(installation.teamId())
                .map(workspace -> {
                    workspace.updateInstallation(
                            installation.teamName(),
                            encryptedBotToken,
                            installation.botUserId(),
                            installation.scope(),
                            installedAt
                    );
                    return workspace;
                })
                .orElseGet(() -> workspaceRepository.save(SlackWorkspace.create(
                        installation.teamId(),
                        installation.teamName(),
                        encryptedBotToken,
                        installation.botUserId(),
                        installation.scope(),
                        installedAt
                )));
    }
}
