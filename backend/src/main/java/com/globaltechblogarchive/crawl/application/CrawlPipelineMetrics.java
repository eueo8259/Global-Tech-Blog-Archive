package com.globaltechblogarchive.crawl.application;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class CrawlPipelineMetrics {

    private static final String SOURCE_TAG = "source";

    private final MeterRegistry meterRegistry;
    private final Timer runTotalTimer;
    private final Timer runPreparationTimer;
    private final Timer runPersistenceTimer;
    private final AtomicInteger persistenceActive = new AtomicInteger();

    public CrawlPipelineMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.runTotalTimer = Timer.builder("crawl.run.total.duration")
                .register(meterRegistry);
        this.runPreparationTimer = Timer.builder("crawl.run.preparation.duration")
                .register(meterRegistry);
        this.runPersistenceTimer = Timer.builder("crawl.run.persistence.duration")
                .register(meterRegistry);
        Gauge.builder(
                        "crawl.source.persistence.active",
                        persistenceActive,
                        AtomicInteger::get
                )
                .register(meterRegistry);
    }

    public <T> T recordCollection(String sourceKey, Supplier<T> action) {
        return Timer.builder("crawl.source.collection.duration")
                .tag(SOURCE_TAG, sourceKey)
                .register(meterRegistry)
                .record(action);
    }

    public <T> T recordPersistence(String sourceKey, Supplier<T> action) {
        persistenceActive.incrementAndGet();
        try {
            return Timer.builder("crawl.source.persistence.duration")
                    .tag(SOURCE_TAG, sourceKey)
                    .register(meterRegistry)
                    .record(action);
        } finally {
            persistenceActive.decrementAndGet();
        }
    }

    public void recordRunDurations(
            Duration totalDuration,
            Duration preparationDuration,
            Duration persistenceDuration
    ) {
        runTotalTimer.record(totalDuration);
        runPreparationTimer.record(preparationDuration);
        runPersistenceTimer.record(persistenceDuration);
    }
}
