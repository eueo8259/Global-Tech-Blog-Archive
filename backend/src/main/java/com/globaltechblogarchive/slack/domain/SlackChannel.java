package com.globaltechblogarchive.slack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "slack_channels",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_slack_channels_workspace_channel",
                columnNames = {"slack_workspace_id", "slack_channel_id"}
        )
)
public class SlackChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slack_workspace_id", nullable = false)
    private SlackWorkspace workspace;

    @Column(name = "slack_channel_id", nullable = false, length = 50)
    private String slackChannelId;

    @Column(name = "slack_channel_name", nullable = false, length = 100)
    private String slackChannelName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SlackChannel create(
            SlackWorkspace workspace,
            String slackChannelId,
            String slackChannelName
    ) {
        SlackChannel channel = new SlackChannel();
        channel.workspace = workspace;
        channel.slackChannelId = slackChannelId;
        channel.slackChannelName = slackChannelName;
        return channel;
    }

    public void rename(String slackChannelName) {
        this.slackChannelName = slackChannelName;
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
