package com.globaltechblogarchive.slack.application;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.slack.application.digest.SlackChatMessage;
import com.globaltechblogarchive.slack.application.digest.SlackDigestArticle;
import com.globaltechblogarchive.slack.application.digest.SlackDigestMessageFactory;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.repository.SlackChannelRepository;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackTestDeliveryService {

    private final CompanyRepository companyRepository;
    private final SlackChannelRepository channelRepository;
    private final SlackDigestMessageFactory messageFactory;
    private final SlackMessageClient messageClient;
    private final SlackTokenEncryptor tokenEncryptor;

    public int send(String companyKey, String title, String articleUrl) {
        Company company = companyRepository.findByCompanyKey(companyKey)
                .orElseThrow(() -> new InvalidInputException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "Unknown company key: " + companyKey
                ));
        List<SlackChannel> channels = channelRepository
                .findSubscribedChannelsWithWorkspaceByCompanyKey(companyKey);
        SlackDigestArticle article = new SlackDigestArticle(
                0L,
                company.getId(),
                company.getCompanyName(),
                title,
                articleUrl,
                LocalDateTime.now()
        );

        for (SlackChannel channel : channels) {
            SlackChatMessage message = messageFactory.create(
                    channel.getSlackChannelId(),
                    List.of(article)
            );
            String botToken = tokenEncryptor.decrypt(
                    channel.getWorkspace().getEncryptedBotToken()
            );
            messageClient.send(botToken, message);
        }
        return channels.size();
    }
}
