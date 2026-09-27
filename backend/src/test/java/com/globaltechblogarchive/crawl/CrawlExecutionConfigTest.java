package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.config.CrawlExecutionConfig;
import com.globaltechblogarchive.crawl.config.CrawlExecutionProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class CrawlExecutionConfigTest {

    private final CrawlExecutionConfig config = new CrawlExecutionConfig();

    @Test
    void crawlSourceExecutorRunsTasksOnNamedPlatformThreads() throws Exception {
        ThreadPoolTaskExecutor executor = config.crawlSourceExecutor(
                new CrawlExecutionProperties(2)
        );
        executor.initialize();
        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(2);
            assertThat(executor.getMaxPoolSize()).isEqualTo(2);
            String threadDescription = executor.submit(() ->
                    Thread.currentThread().getName() + ":" + Thread.currentThread().isVirtual()
            ).get();

            assertThat(threadDescription).startsWith("crawl-source-");
            assertThat(threadDescription).endsWith(":false");
        } finally {
            executor.destroy();
        }
    }

    @Test
    void concurrencyMetricsExposeThreadPoolLimitActiveAndQueuedValues() throws Exception {
        CrawlExecutionProperties properties = new CrawlExecutionProperties(2);
        ThreadPoolTaskExecutor executor = config.crawlSourceExecutor(properties);
        executor.initialize();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        config.crawlSourceConcurrencyMetrics(properties, executor).bindTo(registry);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try {
            for (int task = 0; task < 3; task++) {
                executor.submit(() -> {
                    started.countDown();
                    release.await(2, TimeUnit.SECONDS);
                    return null;
                });
            }

            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(registry.get("crawl.source.concurrency.limit").gauge().value()).isEqualTo(2);
            assertThat(registry.get("crawl.source.concurrency.active").gauge().value()).isEqualTo(2);
            assertThat(registry.get("crawl.source.concurrency.queued").gauge().value()).isEqualTo(1);
        } finally {
            release.countDown();
            executor.destroy();
        }
    }

    @Test
    void persistenceLimiterAllowsOnlyOneWriter() {
        Semaphore limiter = config.crawlSourcePersistenceLimiter();

        assertThat(limiter.availablePermits()).isEqualTo(1);
        limiter.acquireUninterruptibly();
        assertThat(limiter.availablePermits()).isZero();
    }
}
