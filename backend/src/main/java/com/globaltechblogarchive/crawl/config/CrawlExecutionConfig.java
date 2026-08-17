package com.globaltechblogarchive.crawl.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.concurrent.Semaphore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CrawlExecutionConfig {

    @Bean(name = "crawlSourceExecutor")
    public ThreadPoolTaskExecutor crawlSourceExecutor(CrawlExecutionProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.sourceConcurrency());
        executor.setMaxPoolSize(properties.sourceConcurrency());
        executor.setThreadNamePrefix("crawl-source-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        return executor;
    }

    @Bean
    public Semaphore crawlSourcePersistenceLimiter() {
        return new Semaphore(1, true);
    }

    @Bean
    public MeterBinder crawlSourceConcurrencyMetrics(
            CrawlExecutionProperties properties,
            ThreadPoolTaskExecutor crawlSourceExecutor
    ) {
        return registry -> {
            Gauge.builder(
                            "crawl.source.concurrency.active",
                            crawlSourceExecutor,
                            ThreadPoolTaskExecutor::getActiveCount
                    )
                    .register(registry);
            Gauge.builder(
                            "crawl.source.concurrency.queued",
                            crawlSourceExecutor,
                            executor -> executor.getThreadPoolExecutor().getQueue().size()
                    )
                    .register(registry);
            Gauge.builder(
                            "crawl.source.concurrency.limit",
                            properties,
                            CrawlExecutionProperties::sourceConcurrency
                    )
                    .register(registry);
        };
    }
}
