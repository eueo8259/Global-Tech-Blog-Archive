package com.globaltechblogarchive.slack.application;

import com.globaltechblogarchive.slack.api.dto.SlackSlashCommand;
import com.globaltechblogarchive.slack.application.modal.SlackModalMetadata;
import com.globaltechblogarchive.slack.application.modal.SlackModalView;
import com.globaltechblogarchive.slack.application.modal.SlackSubscriptionModalFactory;
import com.globaltechblogarchive.slack.exception.SlackTokenDecryptionException;
import com.globaltechblogarchive.slack.exception.SlackViewException;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackCommandService {

    private static final String WORKSPACE_NOT_FOUND_MESSAGE = "TechPort 앱 설치 정보를 찾을 수 없습니다. 앱을 다시 설치해 주세요.";
    private static final String NO_COMPANIES_MESSAGE = "현재 구독 가능한 회사가 없습니다.";
    private static final String MODAL_OPEN_FAILED_MESSAGE = "구독 설정 화면을 열지 못했습니다. 잠시 후 다시 시도해 주세요.";

    private final SlackSubscriptionQueryService queryService;
    private final SlackTokenEncryptor tokenEncryptor;
    private final SlackSubscriptionModalFactory modalFactory;
    private final SlackViewClient slackViewClient;

    public SlackCommandResult openSubscriptionModal(SlackSlashCommand command) {
        long startedAt = System.nanoTime();
        try {
            Optional<SlackSubscriptionModalData> modalData = queryService.findModalData(
                    command.teamId(),
                    command.channelId()
            );
            if (modalData.isEmpty()) {
                return SlackCommandResult.failure(WORKSPACE_NOT_FOUND_MESSAGE);
            }
            if (modalData.get().companies().isEmpty()) {
                return SlackCommandResult.failure(NO_COMPANIES_MESSAGE);
            }

            String botToken = tokenEncryptor.decrypt(modalData.get().encryptedBotToken());
            SlackModalMetadata metadata = new SlackModalMetadata(
                    command.teamId(),
                    command.channelId(),
                    command.channelName()
            );
            SlackModalView view = modalFactory.create(metadata, modalData.get().companies());
            slackViewClient.open(botToken, command.triggerId(), view);
            return SlackCommandResult.success();
        } catch (DataAccessException | SlackTokenDecryptionException | SlackViewException exception) {
            log.warn(
                    "Slack subscription modal failed: teamId={}, channelId={}, reason={}",
                    command.teamId(),
                    command.channelId(),
                    exception.getMessage()
            );
            return SlackCommandResult.failure(MODAL_OPEN_FAILED_MESSAGE);
        } finally {
            long totalMillis = (System.nanoTime() - startedAt) / 1_000_000;
            log.info(
                    "Slack subscribe command completed: teamId={}, channelId={}, totalMs={}",
                    command.teamId(),
                    command.channelId(),
                    totalMillis
            );
        }
    }
}
