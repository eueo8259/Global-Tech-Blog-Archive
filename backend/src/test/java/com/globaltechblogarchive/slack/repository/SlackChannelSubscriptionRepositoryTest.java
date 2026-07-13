package com.globaltechblogarchive.slack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackChannelSubscription;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class SlackChannelSubscriptionRepositoryTest extends MySqlIntegrationTest {

    @Autowired
    private SlackChannelSubscriptionRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findCompanyKeysByTeamIdAndChannelIdReturnsOnlyCurrentChannelSubscriptions() {
        SlackWorkspace workspace = persistWorkspace();
        SlackChannel targetChannel = persistChannel(workspace, "C123", "tech-news");
        SlackChannel otherChannel = persistChannel(workspace, "C999", "other");
        Company netflix = persistCompany("test-netflix", "Netflix");
        Company uber = persistCompany("test-uber", "Uber");
        entityManager.persist(SlackChannelSubscription.create(targetChannel, netflix));
        entityManager.persist(SlackChannelSubscription.create(otherChannel, uber));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findCompanyKeysByTeamIdAndChannelId("T123", "C123"))
                .containsExactly("test-netflix");
    }

    private SlackWorkspace persistWorkspace() {
        SlackWorkspace workspace = SlackWorkspace.create(
                "T123",
                "TechPort",
                "encrypted-token",
                "B123",
                "commands",
                LocalDateTime.of(2026, 7, 13, 10, 0)
        );
        entityManager.persist(workspace);
        return workspace;
    }

    private SlackChannel persistChannel(SlackWorkspace workspace, String channelId, String channelName) {
        SlackChannel channel = SlackChannel.create(workspace, channelId, channelName);
        entityManager.persist(channel);
        return channel;
    }

    private Company persistCompany(String companyKey, String companyName) {
        Company company = Company.create(companyKey, companyName);
        entityManager.persist(company);
        return company;
    }
}
