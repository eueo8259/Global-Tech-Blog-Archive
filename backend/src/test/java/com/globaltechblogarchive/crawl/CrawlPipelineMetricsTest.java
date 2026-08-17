package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.crawl.application.CrawlPipelineMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class CrawlPipelineMetricsTest {

    @Test
    void recordsCollectionAndPersistenceDurations() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CrawlPipelineMetrics metrics = new CrawlPipelineMetrics(registry);

        String collected = metrics.recordCollection(() -> "collected");
        String persisted = metrics.recordPersistence(() -> {
            assertThat(registry.get("crawl.source.persistence.active").gauge().value())
                    .isEqualTo(1);
            return "persisted";
        });

        assertThat(collected).isEqualTo("collected");
        assertThat(persisted).isEqualTo("persisted");
        assertThat(registry.get("crawl.source.collection.duration").timer().count())
                .isEqualTo(1);
        assertThat(registry.get("crawl.source.persistence.duration").timer().count())
                .isEqualTo(1);
        assertThat(registry.get("crawl.source.persistence.active").gauge().value())
                .isZero();
    }

    @Test
    void persistenceGaugeReturnsToZeroWhenActionFails() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CrawlPipelineMetrics metrics = new CrawlPipelineMetrics(registry);

        assertThatThrownBy(() -> metrics.recordPersistence(() -> {
            throw new IllegalStateException("database failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(registry.get("crawl.source.persistence.active").gauge().value())
                .isZero();
    }
}
