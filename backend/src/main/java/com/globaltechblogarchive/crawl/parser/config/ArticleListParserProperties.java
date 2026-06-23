package com.globaltechblogarchive.crawl.parser.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crawl.parser")
public record ArticleListParserProperties(
        List<String> blockedLinkParts,
        List<String> blockedTitles,
        Map<String, ParserConfig> configs
) {

    public record ParserConfig(
            List<String> articlePathSignals,
            List<String> requiredTextSignals,
            List<String> contextSignals
    ) {
        public ParserConfig {
            if (articlePathSignals == null) articlePathSignals = List.of();
            if (requiredTextSignals == null) requiredTextSignals = List.of();
            if (contextSignals == null) contextSignals = List.of();
        }
    }
}