package com.globaltechblogarchive.collection.parser;

import com.globaltechblogarchive.collection.support.TextCleaner;
import com.globaltechblogarchive.collection.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredArticleListParser implements ArticleListParser {

    private static final Pattern ARTICLE_BLOCK = Pattern.compile(
            "(?is)<(article|li|div)[^>]*(article|post|card|entry|blog)[^>]*>.*?</\\1>"
    );
    private static final Pattern LINK = Pattern.compile("(?is)<a\\b([^>]*)href=[\"']([^\"']+)[\"']([^>]*)>(.*?)</a>");
    private static final Pattern HEADING = Pattern.compile("(?is)<h[1-4]\\b[^>]*>(.*?)</h[1-4]>");
    private static final Pattern ALT = Pattern.compile("(?is)\\balt=[\"']([^\"']+)[\"']");
    private static final Pattern ARIA_LABEL = Pattern.compile("(?is)\\baria-label=[\"']([^\"']+)[\"']");
    private static final Pattern TITLE_ATTRIBUTE = Pattern.compile("(?is)\\btitle=[\"']([^\"']+)[\"']");
    private static final Pattern TIME = Pattern.compile("(?is)<time\\b[^>]*(?:datetime=[\"']([^\"']+)[\"'])?[^>]*>(.*?)</time>");
    private static final Pattern DATE_TEXT = Pattern.compile(
            "(?i)(\\b\\w+\\s+\\d{1,2},\\s+\\d{4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b)"
    );
    private static final List<String> BLOCKED_LINK_PARTS = List.of(
            "mailto:", "javascript:", "/careers", "/jobs", "/contact", "/privacy", "/terms",
            "linkedin.com", "twitter.com", "facebook.com", "instagram.com", "youtube.com",
            "x.com", "dash.cloudflare.com", "devdegree.ca", "shopify.github.io",
            "/rss", "/feed", "/subscribe", "/tag/", "/category/", "/author/", "/topics/",
            "/search", "/start", "/page/"
    );
    private static final List<String> BLOCKED_TITLES = List.of(
            "blog", "engineering", "ai", "database", "security", "placeholder", "get started free",
            "start your business . build your brand", "open source at shopify", "dev degree",
            "shopify engineering on x", "the cloudflare blog", "maker stories"
    );
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    );

    private final Map<String, ParserConfig> configs = createConfigs();

    @Override
    public boolean supports(BlogSource source) {
        return configs.containsKey(source.getCompanyKey());
    }

    @Override
    public List<ParsedArticleCard> parse(BlogSource source, String html) {
        ParserConfig config = configs.get(source.getCompanyKey());
        if (config == null) {
            throw new IllegalArgumentException("No parser config for source: " + source.getCompanyKey());
        }

        Map<String, ParsedArticleCard> cards = new LinkedHashMap<>();
        for (String block : articleBlocks(html)) {
            Optional<ParsedArticleCard> card = parseBlock(source, config, block);
            card.ifPresent(value -> cards.putIfAbsent(UrlNormalizer.normalize(value.originalUrl()), value));
        }
        if (cards.isEmpty()) {
            for (ParsedArticleCard card : parseLinks(source, config, html)) {
                cards.putIfAbsent(UrlNormalizer.normalize(card.originalUrl()), card);
            }
        }
        return new ArrayList<>(cards.values());
    }

    private List<String> articleBlocks(String html) {
        Matcher matcher = ARTICLE_BLOCK.matcher(html);
        List<String> blocks = new ArrayList<>();
        while (matcher.find()) {
            blocks.add(matcher.group());
        }
        return blocks;
    }

    private Optional<ParsedArticleCard> parseBlock(BlogSource source, ParserConfig config, String block) {
        Matcher matcher = LINK.matcher(block);
        while (matcher.find()) {
            String attrs = matcher.group(1) + " " + matcher.group(3);
            String href = matcher.group(2).trim();
            String title = articleTitle(attrs, block, matcher.group(4));
            if (!isArticleLink(source, config, href, title, block)) {
                continue;
            }
            String absoluteUrl = UrlNormalizer.absolute(source.getSiteUrl(), href);
            String context = buildContext(config, block, title);
            return Optional.of(new ParsedArticleCard(title, absoluteUrl, parseDate(block), context));
        }
        return Optional.empty();
    }

    private List<ParsedArticleCard> parseLinks(BlogSource source, ParserConfig config, String html) {
        Matcher matcher = LINK.matcher(html);
        List<ParsedArticleCard> cards = new ArrayList<>();
        while (matcher.find()) {
            String attrs = matcher.group(1) + " " + matcher.group(3);
            String href = matcher.group(2).trim();
            String contextWindow = contextWindow(html, matcher.start(), matcher.end());
            String title = articleTitle(attrs, contextWindow, matcher.group(4));
            if (!isArticleLink(source, config, href, title, contextWindow)) {
                continue;
            }
            String absoluteUrl = UrlNormalizer.absolute(source.getSiteUrl(), href);
            String context = buildContext(config, contextWindow, title);
            cards.add(new ParsedArticleCard(title, absoluteUrl, parseDate(contextWindow), context));
        }
        return cards;
    }

    private String contextWindow(String html, int start, int end) {
        int contextStart = Math.max(0, start - 600);
        int contextEnd = Math.min(html.length(), end + 800);
        return html.substring(contextStart, contextEnd);
    }

    private boolean isArticleLink(BlogSource source, ParserConfig config, String href, String title, String block) {
        if (href.isBlank() || title.isBlank()) {
            return false;
        }
        String lowerHref = href.toLowerCase(Locale.ROOT);
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        String absoluteUrl = UrlNormalizer.absolute(source.getSiteUrl(), href);
        if (UrlNormalizer.normalize(source.getSiteUrl()).equals(UrlNormalizer.normalize(absoluteUrl))) {
            return false;
        }
        if (title.length() < 12 || BLOCKED_TITLES.contains(lowerTitle) || lowerTitle.matches("\\d+")) {
            return false;
        }
        for (String blocked : BLOCKED_LINK_PARTS) {
            if (lowerHref.contains(blocked)) {
                return false;
            }
        }
        boolean pathMatches = config.articlePathSignals().isEmpty()
                || config.articlePathSignals().stream().anyMatch(lowerHref::contains);
        boolean textMatches = config.requiredTextSignals().isEmpty()
                || config.requiredTextSignals().stream().anyMatch(signal ->
                lowerTitle.contains(signal) || block.toLowerCase(Locale.ROOT).contains(signal));
        return pathMatches && textMatches;
    }

    private String articleTitle(String attrs, String block, String linkText) {
        String title = TextCleaner.clean(linkText);
        if (title.isBlank()) {
            title = attributeValue(linkText, ALT);
        }
        if (title.isBlank()) {
            title = attributeValue(attrs, ARIA_LABEL);
        }
        if (title.isBlank()) {
            title = attributeValue(attrs, TITLE_ATTRIBUTE);
        }
        if (!title.isBlank()
                && !title.equalsIgnoreCase("Visit Link")
                && !title.equalsIgnoreCase("Read More")) {
            return title;
        }
        Matcher headingMatcher = HEADING.matcher(block);
        if (headingMatcher.find()) {
            return TextCleaner.clean(headingMatcher.group(1));
        }
        return title;
    }

    private String attributeValue(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return TextCleaner.clean(matcher.group(1));
        }
        return "";
    }

    private String buildContext(ParserConfig config, String block, String title) {
        String text = TextCleaner.clean(block);
        for (String signal : config.contextSignals()) {
            if (text.toLowerCase(Locale.ROOT).contains(signal)) {
                return TextCleaner.shortContext(text, title);
            }
        }
        return TextCleaner.shortContext(text, title);
    }

    private LocalDateTime parseDate(String block) {
        Matcher timeMatcher = TIME.matcher(block);
        if (timeMatcher.find()) {
            String value = firstNonBlank(timeMatcher.group(1), timeMatcher.group(2));
            LocalDateTime parsed = parseDateValue(TextCleaner.clean(value));
            if (parsed != null) {
                return parsed;
            }
        }
        Matcher dateMatcher = DATE_TEXT.matcher(TextCleaner.clean(block));
        if (dateMatcher.find()) {
            return parseDateValue(dateMatcher.group(1));
        }
        return null;
    }

    private LocalDateTime parseDateValue(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // Try next supported list-page date shape.
            }
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private Map<String, ParserConfig> createConfigs() {
        Map<String, ParserConfig> result = new LinkedHashMap<>();
        result.put("openai", new ParserConfig(List.of("/news/"), List.of("engineering", "security", "research", "developers"), List.of("engineering", "security", "research", "developers")));
        result.put("anthropic", new ParserConfig(List.of("/news/"), List.of("research", "product", "policy", "announcement"), List.of("research", "product", "policy", "announcement")));
        result.put("figma", new ParserConfig(List.of("/blog/"), List.of("inside figma engineering", "engineering"), List.of("engineering")));
        result.put("uber", new ParserConfig(List.of("/blog/"), List.of(), List.of("engineering")));
        result.put("airbnb", new ParserConfig(List.of("medium.com/airbnb-engineering", "airbnb.tech"), List.of(), List.of("engineering", "data", "mobile", "backend")));
        result.put("stripe", new ParserConfig(List.of("/blog/"), List.of(), List.of("engineering")));
        result.put("cloudflare", new ParserConfig(List.of("/"), List.of("engineering", "developers", "infrastructure", "security", "ai", "reliability", "database", "networking"), List.of("engineering", "developers", "infrastructure", "security", "ai", "reliability", "database", "networking")));
        result.put("linkedin", new ParserConfig(List.of("/blog/"), List.of(), List.of("engineering")));
        result.put("doordash", new ParserConfig(List.of("/engineering-blog/", "/blog/"), List.of(), List.of("backend", "mobile", "data", "culture")));
        result.put("discord", new ParserConfig(List.of("/blog/", "/category/engineering"), List.of(), List.of("engineering", "developers")));
        result.put("shopify", new ParserConfig(List.of("/"), List.of(), List.of("engineering")));
        result.put("datadog", new ParserConfig(List.of("/blog/"), List.of(), List.of("engineering")));
        result.put("amazon-science", new ParserConfig(List.of("/blog/"), List.of(), List.of("machine learning", "robotics", "systems", "ai")));
        return result;
    }

    private record ParserConfig(
            List<String> articlePathSignals,
            List<String> requiredTextSignals,
            List<String> contextSignals
    ) {
    }
}
