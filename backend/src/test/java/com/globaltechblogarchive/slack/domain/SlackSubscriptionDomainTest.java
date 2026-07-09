package com.globaltechblogarchive.slack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SlackSubscriptionDomainTest {

    @Test
    void createWorkspaceStoresInstallationValues() {
        LocalDateTime installedAt = LocalDateTime.of(2026, 7, 9, 10, 0);

        SlackWorkspace workspace = SlackWorkspace.create(
                "T123",
                "TechPort",
                "encrypted-token",
                "B123",
                "chat:write",
                installedAt
        );

        assertThat(workspace.getSlackTeamId()).isEqualTo("T123");
        assertThat(workspace.getSlackTeamName()).isEqualTo("TechPort");
        assertThat(workspace.getEncryptedBotToken()).isEqualTo("encrypted-token");
        assertThat(workspace.getBotUserId()).isEqualTo("B123");
        assertThat(workspace.getScope()).isEqualTo("chat:write");
        assertThat(workspace.getInstalledAt()).isEqualTo(installedAt);
    }

    @Test
    void updateInstallationReplacesWorkspaceInstallationValues() {
        SlackWorkspace workspace = SlackWorkspace.create(
                "T123",
                "Old Name",
                "old-token",
                "B123",
                "chat:write",
                LocalDateTime.of(2026, 7, 9, 10, 0)
        );
        LocalDateTime reinstalledAt = LocalDateTime.of(2026, 7, 10, 10, 0);

        workspace.updateInstallation(
                "New Name",
                "new-token",
                "B456",
                "chat:write,channels:read",
                reinstalledAt
        );

        assertThat(workspace.getSlackTeamId()).isEqualTo("T123");
        assertThat(workspace.getSlackTeamName()).isEqualTo("New Name");
        assertThat(workspace.getEncryptedBotToken()).isEqualTo("new-token");
        assertThat(workspace.getBotUserId()).isEqualTo("B456");
        assertThat(workspace.getScope()).isEqualTo("chat:write,channels:read");
        assertThat(workspace.getInstalledAt()).isEqualTo(reinstalledAt);
    }

    @Test
    void createChannelStoresWorkspaceAndSlackChannelValues() {
        SlackWorkspace workspace = workspace();

        SlackChannel channel = SlackChannel.create(workspace, "C123", "articles");

        assertThat(channel.getWorkspace()).isSameAs(workspace);
        assertThat(channel.getSlackChannelId()).isEqualTo("C123");
        assertThat(channel.getSlackChannelName()).isEqualTo("articles");
    }

    @Test
    void createSubscriptionConnectsChannelAndCompany() {
        SlackChannel channel = SlackChannel.create(workspace(), "C123", "articles");
        Company company = Company.create("openai", "OpenAI");

        SlackChannelSubscription subscription = SlackChannelSubscription.create(channel, company);

        assertThat(subscription.getSlackChannel()).isSameAs(channel);
        assertThat(subscription.getCompany()).isSameAs(company);
    }

    private SlackWorkspace workspace() {
        return SlackWorkspace.create(
                "T123",
                "TechPort",
                "encrypted-token",
                "B123",
                "chat:write",
                LocalDateTime.of(2026, 7, 9, 10, 0)
        );
    }
}
