package com.globaltechblogarchive.slack.application;

import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.slack.application.SlackSubscriptionModalData.CompanyOption;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackChannelSubscriptionRepository;
import com.globaltechblogarchive.slack.repository.SlackWorkspaceRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlackSubscriptionQueryService {

    private final SlackWorkspaceRepository workspaceRepository;
    private final SlackChannelSubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public Optional<SlackSubscriptionModalData> findModalData(String teamId, String channelId) {
        Optional<SlackWorkspace> workspace = workspaceRepository.findBySlackTeamId(teamId);
        if (workspace.isEmpty()) {
            return Optional.empty();
        }

        Set<String> selectedCompanyKeys = new HashSet<>(
                subscriptionRepository.findCompanyKeysByTeamIdAndChannelId(teamId, channelId)
        );
        List<CompanyOption> companies = companyRepository.findAllByOrderByCompanyNameAsc()
                .stream()
                .map(company -> new CompanyOption(
                        company.getCompanyKey(),
                        company.getCompanyName(),
                        selectedCompanyKeys.contains(company.getCompanyKey())
                ))
                .toList();

        return Optional.of(new SlackSubscriptionModalData(
                workspace.get().getEncryptedBotToken(),
                companies
        ));
    }
}
