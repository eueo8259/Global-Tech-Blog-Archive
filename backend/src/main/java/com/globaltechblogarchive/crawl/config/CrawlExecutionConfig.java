package com.globaltechblogarchive.crawl.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrawlExecutionConfig {

    @Bean(name = "crawlSourceExecutor", destroyMethod = "close")
    public ExecutorService crawlSourceExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("crawl-source-", 0).factory()
        );
    }

    @Bean
    public Semaphore crawlSourceConcurrencyLimiter(CrawlExecutionProperties properties) {
        return new Semaphore(properties.sourceConcurrency(), true);
    }

    @Bean
    public MeterBinder crawlSourceConcurrencyMetrics(
            CrawlExecutionProperties properties,
            Semaphore crawlSourceConcurrencyLimiter
    ) {
        return registry -> {
            Gauge.builder(
                            "crawl.source.concurrency.active",
                            crawlSourceConcurrencyLimiter,
                            limiter -> properties.sourceConcurrency() - limiter.availablePermits()
                    )
                    .register(registry);
            Gauge.builder(
                            "crawl.source.concurrency.queued",
                            crawlSourceConcurrencyLimiter,
                            Semaphore::getQueueLength
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
