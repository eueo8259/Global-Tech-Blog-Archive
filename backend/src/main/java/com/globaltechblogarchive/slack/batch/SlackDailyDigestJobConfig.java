package com.globaltechblogarchive.slack.batch;

import com.globaltechblogarchive.slack.application.digest.SlackDeliveryDispatchService;
import com.globaltechblogarchive.slack.application.digest.SlackDeliveryPreparationService;
import com.globaltechblogarchive.slack.application.digest.SlackDeliveryStateService;
import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackDailyDigestJobConfig {

    @Bean
    public Clock slackDailyDigestClock(SlackDailyDigestProperties properties) {
        return Clock.system(ZoneId.of(properties.zone()));
    }

    @Bean
    public Job slackDailyDigestJob(
            JobRepository jobRepository,
            @Qualifier("prepareSlackDeliveriesStep") Step prepareStep,
            @Qualifier("sendSlackDeliveriesStep") Step sendStep
    ) {
        return new JobBuilder("slackDailyDigestJob", jobRepository)
                .start(prepareStep)
                .next(sendStep)
                .build();
    }

    @Bean
    public Job slackDeliveryRetryJob(
            JobRepository jobRepository,
            @Qualifier("recoverSlackDeliveriesStep") Step recoverStep,
            @Qualifier("sendSlackDeliveriesStep") Step sendStep
    ) {
        return new JobBuilder("slackDeliveryRetryJob", jobRepository)
                .start(recoverStep)
                .next(sendStep)
                .build();
    }

    @Bean
    public Step prepareSlackDeliveriesStep(
            JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            SlackDeliveryStateService stateService,
            SlackDeliveryPreparationService preparationService
    ) {
        return new StepBuilder("prepareSlackDeliveriesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String deliveryDateValue = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("deliveryDate")
                            .toString();
                    String windowEndedAtValue = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("windowEndedAt")
                            .toString();
                    LocalDate deliveryDate = LocalDate.parse(deliveryDateValue);
                    LocalDateTime windowEndedAt = LocalDateTime.parse(windowEndedAtValue);
                    int recoveredCount = stateService.recoverStaleProcessing(windowEndedAt);
                    int createdCount = preparationService.prepare(deliveryDate, windowEndedAt);
                    log.info(
                            "Slack Daily Digest Delivery 준비 완료: deliveryDate={}, created={}, recovered={}",
                            deliveryDate,
                            createdCount,
                            recoveredCount
                    );
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step recoverSlackDeliveriesStep(
            JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            SlackDeliveryStateService stateService
    ) {
        return new StepBuilder("recoverSlackDeliveriesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String executionNowValue = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("executionNow")
                            .toString();
                    LocalDateTime executionNow = LocalDateTime.parse(executionNowValue);
                    int recoveredCount = stateService.recoverStaleProcessing(executionNow);
                    log.info("Slack Delivery stale PROCESSING 복구 완료: recovered={}", recoveredCount);
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step sendSlackDeliveriesStep(
            JobRepository jobRepository,
            @Qualifier("readySlackDeliveryReader") ItemReader<Long> reader,
            @Qualifier("slackDeliveryWriter") ItemWriter<Long> writer
    ) {
        return new StepBuilder("sendSlackDeliveriesStep", jobRepository)
                .<Long, Long>chunk(1, new ResourcelessTransactionManager())
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Long> readySlackDeliveryReader(
            SlackDeliveryRepository deliveryRepository,
            @Value("#{jobParameters['executionNow']}") String executionNowValue
    ) {
        LocalDateTime executionNow = LocalDateTime.parse(executionNowValue);
        List<Long> deliveryIds = deliveryRepository.findReadyDeliveryIds(
                SlackDeliveryStatus.PENDING,
                SlackDeliveryStatus.RETRY_WAITING,
                executionNow
        );
        return new ListItemReader<>(deliveryIds);
    }

    @Bean
    @StepScope
    public ItemWriter<Long> slackDeliveryWriter(
            SlackDeliveryDispatchService dispatchService,
            @Value("#{jobParameters['executionNow']}") String executionNowValue
    ) {
        LocalDateTime executionNow = LocalDateTime.parse(executionNowValue);
        return chunk -> {
            for (Long deliveryId : chunk.getItems()) {
                dispatchService.dispatch(deliveryId, executionNow);
            }
        };
    }
}
