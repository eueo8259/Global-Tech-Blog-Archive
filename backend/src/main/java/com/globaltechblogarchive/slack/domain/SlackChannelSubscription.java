package com.globaltechblogarchive.slack.domain;

import com.globaltechblogarchive.company.domain.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
        name = "slack_channel_subscriptions",
        indexes = @Index(
                name = "idx_slack_channel_subscriptions_company",
                columnList = "company_id"
        ),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_slack_channel_subscriptions_channel_company",
                columnNames = {"slack_channel_id", "company_id"}
        )
)
public class SlackChannelSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slack_channel_id", nullable = false)
    private SlackChannel slackChannel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static SlackChannelSubscription create(SlackChannel slackChannel, Company company) {
        SlackChannelSubscription subscription = new SlackChannelSubscription();
        subscription.slackChannel = slackChannel;
        subscription.company = company;
        return subscription;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
