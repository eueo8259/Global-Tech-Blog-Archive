package com.globaltechblogarchive.crawl.application;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class CrawlPipelineMetrics {

    private final Timer collectionTimer;
    private final Timer persistenceTimer;
    private final AtomicInteger persistenceActive = new AtomicInteger();

    public CrawlPipelineMetrics(MeterRegistry meterRegistry) {
        this.collectionTimer = Timer.builder("crawl.source.collection.duration")
                .register(meterRegistry);
        this.persistenceTimer = Timer.builder("crawl.source.persistence.duration")
                .register(meterRegistry);
        Gauge.builder(
                        "crawl.source.persistence.active",
                        persistenceActive,
                        AtomicInteger::get
                )
                .register(meterRegistry);
    }

    public <T> T recordCollection(Supplier<T> action) {
        return collectionTimer.record(action);
    }

    public <T> T recordPersistence(Supplier<T> action) {
        persistenceActive.incrementAndGet();
        try {
            return persistenceTimer.record(action);
        } finally {
            persistenceActive.decrementAndGet();
        }
    }
}
