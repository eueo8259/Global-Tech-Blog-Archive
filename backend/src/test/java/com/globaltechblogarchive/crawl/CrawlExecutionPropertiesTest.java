package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.crawl.config.CrawlExecutionProperties;
import org.junit.jupiter.api.Test;

class CrawlExecutionPropertiesTest {

    @Test
    void sourceConcurrencyAcceptsExperimentRange() {
        assertThat(new CrawlExecutionProperties(2).sourceConcurrency()).isEqualTo(2);
        assertThat(new CrawlExecutionProperties(4).sourceConcurrency()).isEqualTo(4);
        assertThat(new CrawlExecutionProperties(8).sourceConcurrency()).isEqualTo(8);
    }

    @Test
    void sourceConcurrencyRejectsValueOutsideAllowedRange() {
        assertThatThrownBy(() -> new CrawlExecutionProperties(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("crawl.execution.source-concurrency must be between 1 and 32");
        assertThatThrownBy(() -> new CrawlExecutionProperties(33))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("crawl.execution.source-concurrency must be between 1 and 32");
    }
}
