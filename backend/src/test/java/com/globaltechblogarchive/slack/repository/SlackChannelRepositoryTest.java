package com.globaltechblogarchive.slack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackChannelSubscription;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class SlackChannelRepositoryTest extends MySqlIntegrationTest {

    @Autowired
    private SlackChannelRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findSubscribedChannelsReturnsOnlyChannelsSubscribedToCompany() {
        SlackWorkspace workspace = SlackWorkspace.create(
                "T-TEST-DELIVERY",
                "Test Workspace",
                "encrypted-token",
                "B-TEST",
                "commands,chat:write",
                LocalDateTime.now()
        );
        entityManager.persist(workspace);
        SlackChannel airbnbChannel = SlackChannel.create(workspace, "C-AIRBNB", "airbnb-news");
        SlackChannel netflixChannel = SlackChannel.create(workspace, "C-NETFLIX", "netflix-news");
        entityManager.persist(airbnbChannel);
        entityManager.persist(netflixChannel);
        Company airbnb = Company.create("test-airbnb", "Airbnb");
        Company netflix = Company.create("test-netflix", "Netflix");
        entityManager.persist(airbnb);
        entityManager.persist(netflix);
        entityManager.persist(SlackChannelSubscription.create(airbnbChannel, airbnb));
        entityManager.persist(SlackChannelSubscription.create(netflixChannel, netflix));
        entityManager.flush();
        entityManager.clear();

        List<SlackChannel> channels = repository
                .findSubscribedChannelsWithWorkspaceByCompanyKey("test-airbnb");

        assertThat(channels).extracting(SlackChannel::getSlackChannelId)
                .containsExactly("C-AIRBNB");
        assertThat(channels.getFirst().getWorkspace().getSlackTeamId())
                .isEqualTo("T-TEST-DELIVERY");
    }
}
