package com.globaltechblogarchive.crawl.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ArticleDateParserTest {

    @Test
    void parseListPageDateSupportsStripeDotSeparatedDate() {
        LocalDateTime parsed = ArticleDateParser.parseListPageDate("2026.5.27");

        assertThat(parsed).isEqualTo(LocalDateTime.of(2026, 5, 27, 0, 0));
    }
}
