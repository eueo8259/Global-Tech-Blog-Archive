package com.globaltechblogarchive.crawl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crawl.execution")
public record CrawlExecutionProperties(int sourceConcurrency) {

    private static final int MAX_SOURCE_CONCURRENCY = 32;

    public CrawlExecutionProperties {
        if (sourceConcurrency < 1 || sourceConcurrency > MAX_SOURCE_CONCURRENCY) {
            throw new IllegalArgumentException(
                    "crawl.execution.source-concurrency must be between 1 and "
                            + MAX_SOURCE_CONCURRENCY
            );
        }
    }
}
