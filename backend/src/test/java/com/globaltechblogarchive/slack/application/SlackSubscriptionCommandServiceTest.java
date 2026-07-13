package com.globaltechblogarchive.slack.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.slack.api.dto.SlackViewSubmission;
import com.globaltechblogarchive.slack.application.modal.SlackModalMetadata;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackChannelSubscription;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.exception.InvalidSlackCompanySelectionException;
import com.globaltechblogarchive.slack.repository.SlackChannelRepository;
import com.globaltechblogarchive.slack.repository.SlackChannelSubscriptionRepository;
import com.globaltechblogarchive.slack.repository.SlackWorkspaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SlackSubscriptionCommandServiceTest {

    private final SlackWorkspaceRepository workspaceRepository = mock(SlackWorkspaceRepository.class);
    private final SlackChannelRepository channelRepository = mock(SlackChannelRepository.class);
    private final SlackChannelSubscriptionRepository subscriptionRepository =
            mock(SlackChannelSubscriptionRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final SlackSubscriptionCommandService service = new SlackSubscriptionCommandService(
            workspaceRepository,
            channelRepository,
            subscriptionRepository,
            companyRepository
    );

    private SlackWorkspace workspace;

    @BeforeEach
    void setUp() {
        workspace = SlackWorkspace.create(
                "T123", "TechPort", "encrypted", "B123", "commands", LocalDateTime.now()
        );
        when(workspaceRepository.findBySlackTeamIdForUpdate("T123")).thenReturn(Optional.of(workspace));
    }

    @Test
    void updateSubscriptionsCreatesChannelAndAddsSubscriptions() {
        Company netflix = Company.create("netflix", "Netflix");
        when(channelRepository.findByWorkspaceAndSlackChannelId(workspace, "C123"))
                .thenReturn(Optional.empty());
        when(channelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.findByCompanyKeyIn(Set.of("netflix"))).thenReturn(List.of(netflix));
        when(subscriptionRepository.findAllBySlackChannel(any())).thenReturn(List.of());

        service.updateSubscriptions(submission(Set.of("netflix"), "tech-news"));

        ArgumentCaptor<SlackChannel> channelCaptor = ArgumentCaptor.forClass(SlackChannel.class);
        verify(channelRepository).save(channelCaptor.capture());
        verify(subscriptionRepository).saveAll(any());
        org.assertj.core.api.Assertions.assertThat(channelCaptor.getValue().getSlackChannelName())
                .isEqualTo("tech-news");
    }

    @Test
    void updateSubscriptionsReusesChannelRenamesItAndAppliesBothDiffs() {
        SlackChannel channel = SlackChannel.create(workspace, "C123", "old-name");
        Company netflix = Company.create("netflix", "Netflix");
        Company uber = Company.create("uber", "Uber");
        when(channelRepository.findByWorkspaceAndSlackChannelId(workspace, "C123"))
                .thenReturn(Optional.of(channel));
        when(companyRepository.findByCompanyKeyIn(Set.of("uber"))).thenReturn(List.of(uber));
        when(subscriptionRepository.findAllBySlackChannel(channel))
                .thenReturn(List.of(SlackChannelSubscription.create(channel, netflix)));

        service.updateSubscriptions(submission(Set.of("uber"), "new-name"));

        org.assertj.core.api.Assertions.assertThat(channel.getSlackChannelName()).isEqualTo("new-name");
        verify(channelRepository, never()).save(any());
        verify(subscriptionRepository).saveAll(any());
        verify(subscriptionRepository).deleteAll(any());
    }

    @Test
    void updateSubscriptionsDoesNothingWhenSelectionIsUnchanged() {
        SlackChannel channel = SlackChannel.create(workspace, "C123", "tech-news");
        Company netflix = Company.create("netflix", "Netflix");
        when(channelRepository.findByWorkspaceAndSlackChannelId(workspace, "C123"))
                .thenReturn(Optional.of(channel));
        when(companyRepository.findByCompanyKeyIn(Set.of("netflix"))).thenReturn(List.of(netflix));
        when(subscriptionRepository.findAllBySlackChannel(channel))
                .thenReturn(List.of(SlackChannelSubscription.create(channel, netflix)));

        service.updateSubscriptions(submission(Set.of("netflix"), "tech-news"));

        verify(subscriptionRepository, never()).saveAll(any());
        verify(subscriptionRepository, never()).deleteAll(any());
    }

    @Test
    void updateSubscriptionsDeletesAllSubscriptionsForEmptySelection() {
        SlackChannel channel = SlackChannel.create(workspace, "C123", "tech-news");
        Company netflix = Company.create("netflix", "Netflix");
        when(channelRepository.findByWorkspaceAndSlackChannelId(workspace, "C123"))
                .thenReturn(Optional.of(channel));
        when(companyRepository.findByCompanyKeyIn(Set.of())).thenReturn(List.of());
        when(subscriptionRepository.findAllBySlackChannel(channel))
                .thenReturn(List.of(SlackChannelSubscription.create(channel, netflix)));

        service.updateSubscriptions(submission(Set.of(), "tech-news"));

        verify(subscriptionRepository).deleteAll(any());
        verify(channelRepository, never()).delete(any());
    }

    @Test
    void updateSubscriptionsStopsWhenWorkspaceDoesNotExist() {
        when(workspaceRepository.findBySlackTeamIdForUpdate("T123")).thenReturn(Optional.empty());

        service.updateSubscriptions(submission(Set.of("netflix"), "tech-news"));

        verify(channelRepository, never()).findByWorkspaceAndSlackChannelId(any(), any());
        verify(subscriptionRepository, never()).saveAll(any());
    }

    @Test
    void updateSubscriptionsRejectsUnknownCompanyWithoutChangingSubscriptions() {
        SlackChannel channel = SlackChannel.create(workspace, "C123", "tech-news");
        when(channelRepository.findByWorkspaceAndSlackChannelId(workspace, "C123"))
                .thenReturn(Optional.of(channel));
        when(companyRepository.findByCompanyKeyIn(Set.of("missing"))).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateSubscriptions(submission(Set.of("missing"), "tech-news")))
                .isInstanceOf(InvalidSlackCompanySelectionException.class);

        verify(subscriptionRepository, never()).findAllBySlackChannel(any());
        verify(subscriptionRepository, never()).saveAll(any());
        verify(subscriptionRepository, never()).deleteAll(any());
    }

    private SlackViewSubmission submission(Set<String> companyKeys, String channelName) {
        return new SlackViewSubmission(
                new SlackModalMetadata("T123", "C123", channelName),
                companyKeys
        );
    }
}
