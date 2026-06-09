package com.globaltechblogarchive.crawl.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    @Test
    void normalizeRemovesTrackingQueryParameters() {
        String normalized = UrlNormalizer.normalize(
                "https://netflixtechblog.com/post?source=rss----2615bd06b42e---4&utm_source=feed&fbclid=abc"
        );

        assertThat(normalized).isEqualTo("https://netflixtechblog.com/post");
    }

    @Test
    void normalizeKeepsMeaningfulQueryParameters() {
        String normalized = UrlNormalizer.normalize(
                "https://example.com/article?id=123&utm_medium=email&page=2"
        );

        assertThat(normalized).isEqualTo("https://example.com/article?id=123&page=2");
    }

    @Test
    void normalizeLowercasesHostAndRemovesFragment() {
        String normalized = UrlNormalizer.normalize(
                "HTTPS://Example.com/a/../post#section"
        );

        assertThat(normalized).isEqualTo("https://example.com/post");
    }
}
