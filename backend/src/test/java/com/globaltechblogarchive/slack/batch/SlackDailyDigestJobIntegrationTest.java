package com.globaltechblogarchive.slack.batch;

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
import com.globaltechblogarchive.slack.domain.SlackDelivery;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "slack.bot-token-encryption.key-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "slack.daily-digest.enabled=false"
})
class SlackDailyDigestJobIntegrationTest extends MySqlIntegrationTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2030, 1, 2);
    private static final LocalDateTime WINDOW_END = DELIVERY_DATE.atTime(9, 0);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("slackDailyDigestJob")
    private Job dailyDigestJob;

    @Autowired
    @Qualifier("slackDeliveryRetryJob")
    private Job retryJob;

    @Autowired
    private SlackDeliveryRepository deliveryRepository;

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
    void dailyJobSendsOnceAndRetryJobDoesNotResendSentDelivery() throws Exception {
        persistDigestTarget();
        when(messageClient.send(eq("xoxb-batch-test"), any()))
                .thenReturn(new SlackMessageSendResult("123.456"));

        JobExecution dailyExecution = jobLauncher.run(dailyDigestJob, dailyParameters());

        assertThat(dailyExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        SlackDelivery delivery = deliveryRepository.findAll().stream()
                .filter(candidate -> candidate.getDeliveryDate().equals(DELIVERY_DATE))
                .findFirst()
                .orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.SENT);
        assertThat(delivery.getSlackMessageTs()).isEqualTo("123.456");
        verify(messageClient).send(eq("xoxb-batch-test"), any());

        JobExecution retryExecution = jobLauncher.run(retryJob, retryParameters());

        assertThat(retryExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verifyNoMoreInteractions(messageClient);
    }

    private void persistDigestTarget() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            Company company = Company.create("batch-company", "Batch Company");
            entityManager.persist(company);
            SlackWorkspace workspace = SlackWorkspace.create(
                    "T-BATCH",
                    "Batch Workspace",
                    tokenEncryptor.encrypt("xoxb-batch-test"),
                    "B-BATCH",
                    "commands,chat:write",
                    LocalDateTime.of(2026, 1, 1, 0, 0)
            );
            entityManager.persist(workspace);
            SlackChannel channel = SlackChannel.create(workspace, "C-BATCH", "batch-digest");
            entityManager.persist(channel);
            entityManager.persist(SlackChannelSubscription.create(channel, company));
            entityManager.flush();

            LocalDateTime articleCreatedAt = LocalDateTime.of(2030, 1, 2, 5, 0);
            entityManager.persist(Article.restore(
                    company,
                    "Batch Article",
                    "https://example.com/batch-article",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    ArticleCategory.BACKEND,
                    articleCreatedAt,
                    articleCreatedAt,
                    articleCreatedAt
            ));
        });
    }

    private JobParameters dailyParameters() {
        return new JobParametersBuilder()
                .addString("deliveryDate", DELIVERY_DATE.toString(), true)
                .addString("windowEndedAt", WINDOW_END.toString(), false)
                .addString("executionNow", WINDOW_END.toString(), false)
                .toJobParameters();
    }

    private JobParameters retryParameters() {
        LocalDateTime retryAt = WINDOW_END.plusMinutes(5);
        return new JobParametersBuilder()
                .addString("retrySlot", "2030-01-02T09:05+09:00[Asia/Seoul]", true)
                .addString("executionNow", retryAt.toString(), false)
                .toJobParameters();
    }
}
