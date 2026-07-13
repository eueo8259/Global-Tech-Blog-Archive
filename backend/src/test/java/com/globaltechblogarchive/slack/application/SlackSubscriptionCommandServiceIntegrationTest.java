package com.globaltechblogarchive.slack.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.slack.api.dto.SlackViewSubmission;
import com.globaltechblogarchive.slack.application.modal.SlackModalMetadata;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackChannelRepository;
import com.globaltechblogarchive.slack.repository.SlackChannelSubscriptionRepository;
import com.globaltechblogarchive.slack.repository.SlackWorkspaceRepository;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class SlackSubscriptionCommandServiceIntegrationTest extends MySqlIntegrationTest {

    @Autowired
    private SlackWorkspaceRepository workspaceRepository;

    @Autowired
    private SlackChannelRepository channelRepository;

    @Autowired
    private SlackChannelSubscriptionRepository subscriptionRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TestEntityManager entityManager;

    private SlackSubscriptionCommandService service;

    @BeforeEach
    void setUp() {
        service = new SlackSubscriptionCommandService(
                workspaceRepository,
                channelRepository,
                subscriptionRepository,
                companyRepository
        );
    }

    @Test
    void updateSubscriptionsPersistsIdempotentlyAndDeletesDeselectedRows() {
        SlackWorkspace workspace = SlackWorkspace.create(
                "T-SUBMIT",
                "TechPort",
                "encrypted-token",
                "B123",
                "commands",
                LocalDateTime.of(2026, 7, 13, 10, 0)
        );
        entityManager.persist(workspace);
        entityManager.persist(Company.create("submit-netflix", "Netflix"));
        entityManager.persist(Company.create("submit-uber", "Uber"));
        entityManager.flush();
        entityManager.clear();

        service.updateSubscriptions(submission(Set.of("submit-netflix", "submit-uber")));
        entityManager.flush();
        entityManager.clear();

        SlackWorkspace savedWorkspace = workspaceRepository.findBySlackTeamId("T-SUBMIT").orElseThrow();
        SlackChannel channel = channelRepository.findByWorkspaceAndSlackChannelId(savedWorkspace, "C-SUBMIT")
                .orElseThrow();
        assertThat(subscriptionRepository.findAllBySlackChannel(channel))
                .extracting(subscription -> subscription.getCompany().getCompanyKey())
                .containsExactlyInAnyOrder("submit-netflix", "submit-uber");

        service.updateSubscriptions(submission(Set.of("submit-netflix", "submit-uber")));
        entityManager.flush();
        assertThat(subscriptionRepository.findAllBySlackChannel(channel)).hasSize(2);

        service.updateSubscriptions(submission(Set.of("submit-uber")));
        entityManager.flush();
        assertThat(subscriptionRepository.findAllBySlackChannel(channel))
                .extracting(subscription -> subscription.getCompany().getCompanyKey())
                .containsExactly("submit-uber");

        service.updateSubscriptions(submission(Set.of()));
        entityManager.flush();
        assertThat(subscriptionRepository.findAllBySlackChannel(channel)).isEmpty();
        assertThat(channelRepository.findById(channel.getId())).isPresent();
    }

    private SlackViewSubmission submission(Set<String> selectedCompanyKeys) {
        return new SlackViewSubmission(
                new SlackModalMetadata("T-SUBMIT", "C-SUBMIT", "tech-news"),
                selectedCompanyKeys
        );
    }
}
