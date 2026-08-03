package com.globaltechblogarchive.slack.application.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.slack.application.SlackMessageClient;
import com.globaltechblogarchive.slack.application.SlackMessageSendResult;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackChannelSubscription;
import com.globaltechblogarchive.slack.domain.SlackDailyDigestRun;
import com.globaltechblogarchive.slack.domain.SlackDailyDigestRunStatus;
import com.globaltechblogarchive.slack.domain.SlackDelivery;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackDailyDigestRunRepository;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "slack.bot-token-encryption.key-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "slack.daily-digest.enabled=false"
})
class SlackDailyDigestServiceIntegrationTest extends MySqlIntegrationTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2035, 1, 2);
    private static final LocalDateTime WINDOW_END = DELIVERY_DATE.atTime(9, 0);

    @Autowired
    private SlackDailyDigestService digestService;

    @Autowired
    private SlackDailyDigestRunRepository runRepository;

    @Autowired
    private SlackDeliveryRepository deliveryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SlackTokenEncryptor tokenEncryptor;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SlackMessageClient messageClient;

    @Test
    void dailyRunStoresExecutionAndRetryDoesNotResendSentDelivery() {
        Integer batchTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name LIKE 'BATCH\\_%'
                        """,
                Integer.class
        );
        assertThat(batchTableCount).isEqualTo(9);

        persistDigestTarget();
        when(messageClient.send(eq("xoxb-digest-test"), any()))
                .thenReturn(new SlackMessageSendResult("123.456"));

        SlackDailyDigestRunResult first = digestService.runDaily(
                DELIVERY_DATE,
                WINDOW_END,
                WINDOW_END
        );

        assertThat(first.started()).isTrue();
        SlackDailyDigestRun run = runRepository.findByDeliveryDate(DELIVERY_DATE).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(SlackDailyDigestRunStatus.COMPLETED);
        assertThat(run.getCreatedDeliveryCount()).isEqualTo(1);
        assertThat(run.getReadyDeliveryCount()).isEqualTo(1);

        SlackDelivery delivery = deliveryRepository.findAll().stream()
                .filter(candidate -> candidate.getDeliveryDate().equals(DELIVERY_DATE))
                .findFirst()
                .orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.SENT);
        assertThat(delivery.getSlackMessageTs()).isEqualTo("123.456");
        verify(messageClient).send(eq("xoxb-digest-test"), any());

        SlackDailyDigestRunResult repeated = digestService.runDaily(
                DELIVERY_DATE,
                WINDOW_END,
                WINDOW_END.plusMinutes(1)
        );
        assertThat(repeated.started()).isFalse();

        digestService.runRetry(WINDOW_END.plusMinutes(5));
        verifyNoMoreInteractions(messageClient);
    }

    private void persistDigestTarget() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            Company company = Company.create("run-157-company", "Run 157 Company");
            entityManager.persist(company);
            SlackWorkspace workspace = SlackWorkspace.create(
                    "T-RUN-157",
                    "Run 157 Workspace",
                    tokenEncryptor.encrypt("xoxb-digest-test"),
                    "B-RUN-157",
                    "commands,chat:write",
                    LocalDateTime.of(2034, 1, 1, 0, 0)
            );
            entityManager.persist(workspace);
            SlackChannel channel = SlackChannel.create(workspace, "C-RUN-157", "daily-digest");
            entityManager.persist(channel);
            entityManager.persist(SlackChannelSubscription.create(channel, company));
            entityManager.flush();

            LocalDateTime articleCreatedAt = DELIVERY_DATE.atTime(5, 0);
            entityManager.persist(Article.restore(
                    company,
                    "Digest Article",
                    "https://example.com/digest-article",
                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                    ArticleCategory.BACKEND,
                    articleCreatedAt,
                    articleCreatedAt,
                    articleCreatedAt
            ));
        });
    }
}
