package com.globaltechblogarchive.slack.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.slack.application.digest.SlackChatMessage;
import com.globaltechblogarchive.slack.application.digest.SlackDigestMessageFactory;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackChannelRepository;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SlackTestDeliveryServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final SlackChannelRepository channelRepository = mock(SlackChannelRepository.class);
    private final SlackDigestMessageFactory messageFactory = mock(SlackDigestMessageFactory.class);
    private final SlackMessageClient messageClient = mock(SlackMessageClient.class);
    private final SlackTokenEncryptor tokenEncryptor = mock(SlackTokenEncryptor.class);
    private final SlackTestDeliveryService service = new SlackTestDeliveryService(
            companyRepository,
            channelRepository,
            messageFactory,
            messageClient,
            tokenEncryptor
    );

    @Test
    void sendDeliversSyntheticArticleToSubscribedChannels() {
        Company airbnb = Company.create("airbnb", "Airbnb");
        SlackWorkspace workspace = SlackWorkspace.create(
                "T123",
                "Test Workspace",
                "encrypted-token",
                "B123",
                "commands,chat:write",
                LocalDateTime.now()
        );
        SlackChannel channel = SlackChannel.create(workspace, "C123", "tech-news");
        SlackChatMessage message = new SlackChatMessage(
                "C123",
                "test",
                List.of(),
                SlackChatMessage.Metadata.digest("delivery-key")
        );
        when(companyRepository.findByCompanyKey("airbnb")).thenReturn(Optional.of(airbnb));
        when(channelRepository.findSubscribedChannelsWithWorkspaceByCompanyKey("airbnb"))
                .thenReturn(List.of(channel));
        when(messageFactory.create(eq("C123"), anyString(), anyList())).thenReturn(message);
        when(tokenEncryptor.decrypt("encrypted-token")).thenReturn("xoxb-token");

        int sentCount = service.send("airbnb", "Test article", "https://example.com/article");

        assertThat(sentCount).isEqualTo(1);
        verify(messageFactory).create(eq("C123"), anyString(), argThat(articles ->
                articles.size() == 1
                        && "Airbnb".equals(articles.getFirst().companyName())
                        && "Test article".equals(articles.getFirst().title())
                        && "https://example.com/article".equals(articles.getFirst().articleUrl())
        ));
        verify(messageClient).send("xoxb-token", message);
    }

    @Test
    void sendReturnsZeroWhenNoChannelsSubscribeToCompany() {
        when(companyRepository.findByCompanyKey("airbnb"))
                .thenReturn(Optional.of(Company.create("airbnb", "Airbnb")));
        when(channelRepository.findSubscribedChannelsWithWorkspaceByCompanyKey("airbnb"))
                .thenReturn(List.of());

        int sentCount = service.send("airbnb", "Test article", "https://example.com/article");

        assertThat(sentCount).isZero();
        verify(messageClient, never()).send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void sendRejectsUnknownCompanyKey() {
        when(companyRepository.findByCompanyKey("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(
                "unknown",
                "Test article",
                "https://example.com/article"
        ))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Unknown company key: unknown");
    }
}
