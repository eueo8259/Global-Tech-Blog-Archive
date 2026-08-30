package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.crawl.application.CrawlPipelineMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CrawlPipelineMetricsTest {

    @Test
    void recordsCollectionAndPersistenceDurations() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CrawlPipelineMetrics metrics = new CrawlPipelineMetrics(registry);

        String collected = metrics.recordCollection("github", () -> "collected");
        String persisted = metrics.recordPersistence("github", () -> {
            assertThat(registry.get("crawl.source.persistence.active").gauge().value())
                    .isEqualTo(1);
            return "persisted";
        });
        metrics.recordRunDurations(
                Duration.ofSeconds(3),
                Duration.ofSeconds(2),
                Duration.ofSeconds(1)
        );

        assertThat(collected).isEqualTo("collected");
        assertThat(persisted).isEqualTo("persisted");
        assertThat(registry.get("crawl.source.collection.duration")
                .tag("source", "github")
                .timer()
                .count())
                .isEqualTo(1);
        assertThat(registry.get("crawl.source.persistence.duration")
                .tag("source", "github")
                .timer()
                .count())
                .isEqualTo(1);
        assertThat(registry.get("crawl.source.persistence.active").gauge().value())
                .isZero();
        assertThat(registry.get("crawl.run.total.duration").timer()
                .totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(3);
        assertThat(registry.get("crawl.run.preparation.duration").timer()
                .totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(2);
        assertThat(registry.get("crawl.run.persistence.duration").timer()
                .totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(1);
    }

    @Test
    void persistenceGaugeReturnsToZeroWhenActionFails() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CrawlPipelineMetrics metrics = new CrawlPipelineMetrics(registry);

        assertThatThrownBy(() -> metrics.recordPersistence("github", () -> {
            throw new IllegalStateException("database failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(registry.get("crawl.source.persistence.active").gauge().value())
                .isZero();
    }
}
