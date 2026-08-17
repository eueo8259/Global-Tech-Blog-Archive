package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.config.CrawlExecutionConfig;
import com.globaltechblogarchive.crawl.config.CrawlExecutionProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.Test;

class CrawlExecutionConfigTest {

    private final CrawlExecutionConfig config = new CrawlExecutionConfig();

    @Test
    void crawlSourceExecutorRunsTasksOnNamedVirtualThreads() throws Exception {
        try (ExecutorService executor = config.crawlSourceExecutor()) {
            String threadDescription = executor.submit(() ->
                    Thread.currentThread().getName() + ":" + Thread.currentThread().isVirtual()
            ).get();

            assertThat(threadDescription).startsWith("crawl-source-");
            assertThat(threadDescription).endsWith(":true");
        }
    }

    @Test
    void concurrencyMetricsExposeLimitActiveAndQueuedValues() {
        CrawlExecutionProperties properties = new CrawlExecutionProperties(4);
        Semaphore limiter = config.crawlSourceConcurrencyLimiter(properties);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        config.crawlSourceConcurrencyMetrics(properties, limiter).bindTo(registry);

        limiter.acquireUninterruptibly(2);

        assertThat(registry.get("crawl.source.concurrency.limit").gauge().value()).isEqualTo(4);
        assertThat(registry.get("crawl.source.concurrency.active").gauge().value()).isEqualTo(2);
        assertThat(registry.get("crawl.source.concurrency.queued").gauge().value()).isZero();
    }
}
