package com.globaltechblogarchive.slack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "slack_workspaces")
public class SlackWorkspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slack_team_id", nullable = false, unique = true, length = 50)
    private String slackTeamId;

    @Column(name = "slack_team_name", nullable = false, length = 100)
    private String slackTeamName;

    @Column(name = "encrypted_bot_token", nullable = false, length = 1000)
    private String encryptedBotToken;

    @Column(name = "bot_user_id", length = 50)
    private String botUserId;

    @Column(name = "scope", length = 500)
    private String scope;

    @Column(name = "installed_at", nullable = false)
    private LocalDateTime installedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SlackWorkspace create(
            String slackTeamId,
            String slackTeamName,
            String encryptedBotToken,
            String botUserId,
            String scope,
            LocalDateTime installedAt
    ) {
        SlackWorkspace workspace = new SlackWorkspace();
        workspace.slackTeamId = slackTeamId;
        workspace.slackTeamName = slackTeamName;
        workspace.encryptedBotToken = encryptedBotToken;
        workspace.botUserId = botUserId;
        workspace.scope = scope;
        workspace.installedAt = installedAt;
        return workspace;
    }

    public void updateInstallation(
            String slackTeamName,
            String encryptedBotToken,
            String botUserId,
            String scope,
            LocalDateTime installedAt
    ) {
        this.slackTeamName = slackTeamName;
        this.encryptedBotToken = encryptedBotToken;
        this.botUserId = botUserId;
        this.scope = scope;
        this.installedAt = installedAt;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
