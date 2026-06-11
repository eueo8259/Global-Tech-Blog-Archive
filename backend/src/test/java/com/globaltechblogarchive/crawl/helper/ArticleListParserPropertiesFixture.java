package com.globaltechblogarchive.crawl.helper;

import com.globaltechblogarchive.crawl.parser.config.ArticleListParserProperties;
import com.globaltechblogarchive.crawl.parser.config.ArticleListParserProperties.ParserConfig;


import java.util.List;
import java.util.Map;

public class ArticleListParserPropertiesFixture {

    public static ArticleListParserProperties full() {
        return new ArticleListParserProperties(
                blockedLinkParts(),
                blockedTitles(),
                allConfigs()
        );
    }

    public static ArticleListParserProperties withConfigs(Map<String, ParserConfig> configs) {
        return new ArticleListParserProperties(
                blockedLinkParts(),
                blockedTitles(),
                configs
        );
    }

    private static List<String> blockedLinkParts() {
        return List.of(
                "mailto:", "javascript:", "/careers", "/jobs", "/contact", "/privacy", "/terms",
                "linkedin.com", "twitter.com", "facebook.com", "instagram.com", "youtube.com",
                "x.com", "dash.cloudflare.com", "devdegree.ca", "shopify.github.io",
                "/rss", "/feed", "/subscribe", "/tag/", "/category/", "/author/", "/topics/",
                "/search", "/start", "/page/"
        );
    }

    private static List<String> blockedTitles() {
        return List.of(
                "blog", "engineering", "ai", "database", "security", "placeholder", "get started free",
                "start your business . build your brand", "open source at shopify", "dev degree",
                "shopify engineering on x", "the cloudflare blog", "maker stories"
        );
    }

    private static Map<String, ParserConfig> allConfigs() {
        return Map.ofEntries(
                Map.entry("openai", new ParserConfig(List.of("/news/"), List.of("engineering", "security", "research", "developers"), List.of("engineering", "security", "research", "developers"))),
                Map.entry("anthropic-engineering", new ParserConfig(List.of("/news/"), List.of("research", "product", "policy", "announcement"), List.of("research", "product", "policy", "announcement"))),
                Map.entry("claude-blog", new ParserConfig(List.of("/blog/"), List.of(), List.of("claude code", "agents", "engineering", "developers"))),
                Map.entry("figma", new ParserConfig(List.of("/blog/"), List.of("inside figma engineering", "engineering"), List.of("engineering"))),
                Map.entry("uber", new ParserConfig(List.of("/blog/"), List.of(), List.of("engineering"))),
                Map.entry("airbnb", new ParserConfig(List.of("medium.com/airbnb-engineering", "airbnb.tech"), List.of(), List.of("engineering", "data", "mobile", "backend"))),
                Map.entry("stripe", new ParserConfig(List.of("/blog/"), List.of(), List.of("engineering"))),
                Map.entry("cloudflare", new ParserConfig(List.of("/"), List.of("engineering", "developers", "infrastructure", "security", "ai", "reliability", "database", "networking"), List.of("engineering", "developers", "infrastructure", "security", "ai", "reliability", "database", "networking"))),
                Map.entry("linkedin", new ParserConfig(List.of("/blog/"), List.of(), List.of("engineering"))),
                Map.entry("doordash", new ParserConfig(List.of("/engineering-blog/", "/blog/"), List.of(), List.of("backend", "mobile", "data", "culture"))),
                Map.entry("discord", new ParserConfig(List.of("/blog/", "/category/engineering"), List.of(), List.of("engineering", "developers"))),
                Map.entry("shopify", new ParserConfig(List.of("/"), List.of(), List.of("engineering"))),
                Map.entry("datadog", new ParserConfig(List.of("/blog/"), List.of(), List.of("engineering"))),
                Map.entry("amazon-science", new ParserConfig(List.of("/blog/"), List.of(), List.of("machine learning", "robotics", "systems", "ai")))
        );
    }
}