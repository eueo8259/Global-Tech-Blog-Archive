package com.globaltechblogarchive.slack.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackChannelSubscriptionRepository;
import com.globaltechblogarchive.slack.repository.SlackWorkspaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SlackSubscriptionQueryServiceTest {

    private final SlackWorkspaceRepository workspaceRepository = mock(SlackWorkspaceRepository.class);
    private final SlackChannelSubscriptionRepository subscriptionRepository =
            mock(SlackChannelSubscriptionRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final SlackSubscriptionQueryService service = new SlackSubscriptionQueryService(
            workspaceRepository,
            subscriptionRepository,
            companyRepository
    );

    @Test
    void findModalDataReturnsCompaniesWithExistingSubscriptionsSelected() {
        when(workspaceRepository.findBySlackTeamId("T123")).thenReturn(Optional.of(workspace()));
        when(subscriptionRepository.findCompanyKeysByTeamIdAndChannelId("T123", "C123"))
                .thenReturn(List.of("netflix"));
        when(companyRepository.findAllByOrderByCompanyNameAsc()).thenReturn(List.of(
                Company.create("airbnb", "Airbnb"),
                Company.create("netflix", "Netflix")
        ));

        SlackSubscriptionModalData data = service.findModalData("T123", "C123").orElseThrow();

        assertThat(data.encryptedBotToken()).isEqualTo("encrypted-token");
        assertThat(data.companies())
                .extracting(option -> option.companyKey() + ":" + option.selected())
                .containsExactly("airbnb:false", "netflix:true");
    }

    @Test
    void findModalDataStopsWhenWorkspaceDoesNotExist() {
        when(workspaceRepository.findBySlackTeamId("missing")).thenReturn(Optional.empty());

        assertThat(service.findModalData("missing", "C123")).isEmpty();
        verify(subscriptionRepository, never()).findCompanyKeysByTeamIdAndChannelId("missing", "C123");
        verify(companyRepository, never()).findAllByOrderByCompanyNameAsc();
    }

    private SlackWorkspace workspace() {
        return SlackWorkspace.create(
                "T123",
                "TechPort",
                "encrypted-token",
                "B123",
                "commands",
                LocalDateTime.of(2026, 7, 13, 10, 0)
        );
    }
}
