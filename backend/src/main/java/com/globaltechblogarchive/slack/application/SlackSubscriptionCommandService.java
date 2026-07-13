package com.globaltechblogarchive.slack.application;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.slack.api.dto.SlackViewSubmission;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackChannelSubscription;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.exception.InvalidSlackCompanySelectionException;
import com.globaltechblogarchive.slack.repository.SlackChannelRepository;
import com.globaltechblogarchive.slack.repository.SlackChannelSubscriptionRepository;
import com.globaltechblogarchive.slack.repository.SlackWorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackSubscriptionCommandService {

    private final SlackWorkspaceRepository workspaceRepository;
    private final SlackChannelRepository channelRepository;
    private final SlackChannelSubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public void updateSubscriptions(SlackViewSubmission submission) {
        long startedAt = System.nanoTime();
        try {
            String teamId = submission.metadata().teamId();
            // MVP 트래픽에서는 workspace 단위 직렬화로 충분하며, 트래픽 증가 시 채널 단위 잠금으로 좁힌다.
            SlackWorkspace workspace = workspaceRepository.findBySlackTeamIdForUpdate(teamId)
                    .orElse(null);
            if (workspace == null) {
                log.warn("Slack workspace not found during subscription update: teamId={}", teamId);
                return;
            }

            SlackChannel channel = channelRepository.findByWorkspaceAndSlackChannelId(
                            workspace,
                            submission.metadata().channelId()
                    )
                    .map(existingChannel -> {
                        existingChannel.rename(submission.metadata().channelName());
                        return existingChannel;
                    })
                    .orElseGet(() -> channelRepository.save(SlackChannel.create(
                            workspace,
                            submission.metadata().channelId(),
                            submission.metadata().channelName()
                    )));

            Set<String> selectedKeys = submission.selectedCompanyKeys();
            List<Company> selectedCompanies = companyRepository.findByCompanyKeyIn(selectedKeys);
            if (selectedCompanies.size() != selectedKeys.size()) {
                throw new InvalidSlackCompanySelectionException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "존재하지 않는 회사가 포함되어 있습니다. 다시 시도해 주세요."
                );
            }

            List<SlackChannelSubscription> existingSubscriptions =
                    subscriptionRepository.findAllBySlackChannel(channel);
            Map<String, SlackChannelSubscription> existingByCompanyKey = existingSubscriptions.stream()
                    .collect(Collectors.toMap(
                            subscription -> subscription.getCompany().getCompanyKey(),
                            Function.identity()
                    ));
            Map<String, Company> selectedByCompanyKey = selectedCompanies.stream()
                    .collect(Collectors.toMap(Company::getCompanyKey, Function.identity()));

            List<SlackChannelSubscription> additions = selectedKeys.stream()
                    .filter(companyKey -> !existingByCompanyKey.containsKey(companyKey))
                    .map(companyKey -> SlackChannelSubscription.create(
                            channel,
                            selectedByCompanyKey.get(companyKey)
                    ))
                    .toList();
            List<SlackChannelSubscription> removals = existingSubscriptions.stream()
                    .filter(subscription -> !selectedKeys.contains(
                            subscription.getCompany().getCompanyKey()
                    ))
                    .toList();

            if (!additions.isEmpty()) {
                subscriptionRepository.saveAll(additions);
            }
            if (!removals.isEmpty()) {
                subscriptionRepository.deleteAll(removals);
            }
        } finally {
            long totalMillis = (System.nanoTime() - startedAt) / 1_000_000;
            log.info(
                    "Slack subscription update completed: teamId={}, channelId={}, totalMs={}",
                    submission.metadata().teamId(),
                    submission.metadata().channelId(),
                    totalMillis
            );
        }
    }
}
