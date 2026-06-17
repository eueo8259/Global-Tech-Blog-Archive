package com.globaltechblogarchive.crawl.parser;

import com.globaltechblogarchive.crawl.parser.config.ArticleListParserProperties;
import com.globaltechblogarchive.crawl.parser.config.ArticleListParserProperties.ParserConfig;
import com.globaltechblogarchive.crawl.support.ArticleDateParser;
import com.globaltechblogarchive.crawl.support.TextCleaner;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HtmlArticleListParser implements ArticleListParser {

    private static final List<String> UBER_CATEGORY_PATHS = List.of(
            "/blog/advertising/",
            "/blog/earn/",
            "/blog/ride/",
            "/blog/eat/",
            "/blog/merchants/",
            "/blog/business/",
            "/blog/health/",
            "/blog/higher-education/",
            "/blog/transit/",
            "/blog/engineering/",
            "/blog/community-support/"
    );
    private static final Pattern ARTICLE_BLOCK = Pattern.compile(
            "(?is)<(article|li|div)[^>]*(article|post|card|entry|blog)[^>]*>.*?</\\1>"
    );
    private static final Pattern LINK = Pattern.compile(
            "(?is)<a\\b([^>]*)href=[\"']([^\"']+)[\"']([^>]*)>(.*?)</a>"
    );
    private static final Pattern HEADING = Pattern.compile("(?is)<h[1-4]\\b[^>]*>(.*?)</h[1-4]>");
    private static final Pattern ALT = Pattern.compile("(?is)\\balt=[\"']([^\"']+)[\"']");
    private static final Pattern ARIA_LABEL = Pattern.compile("(?is)\\baria-label=[\"']([^\"']+)[\"']");
    private static final Pattern TITLE_ATTRIBUTE = Pattern.compile("(?is)\\btitle=[\"']([^\"']+)[\"']");
    private static final Pattern TIME = Pattern.compile(
            "(?is)<time\\b[^>]*(?:datetime=[\"']([^\"']+)[\"'])?[^>]*>(.*?)</time>"
    );
    private static final Pattern DATE_TEXT = Pattern.compile(
            "(?i)(\\b\\w+\\s+\\d{1,2},\\s+\\d{4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b)"
    );

    private final ArticleListParserProperties properties;

    @Override
    public boolean supports(BlogSource source) {
        return properties.configs().containsKey(source.getSourceKey());
    }

    @Override
    public List<ParsedArticle> parse(BlogSource source, String html) {
        ParserConfig config = properties.configs().get(source.getSourceKey());
        if (config == null) {
            throw new IllegalArgumentException("No parser config for source: " + source.getSourceKey());
        }

        Map<String, ParsedArticle> cards = new LinkedHashMap<>();
        for (String block : articleBlocks(html)) {
            Optional<ParsedArticle> card = parseBlock(source, config, block);
            card.ifPresent(value -> cards.putIfAbsent(UrlNormalizer.normalize(value.originalUrl()), value));
        }
        if (cards.isEmpty()) {
            for (ParsedArticle card : parseLinks(source, config, html)) {
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

    private Optional<ParsedArticle> parseBlock(BlogSource source, ParserConfig config, String block) {
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
            return Optional.of(new ParsedArticle(title, absoluteUrl, parseDate(block), context));
        }
        return Optional.empty();
    }

    private List<ParsedArticle> parseLinks(BlogSource source, ParserConfig config, String html) {
        Matcher matcher = LINK.matcher(html);
        List<ParsedArticle> cards = new ArrayList<>();
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
            cards.add(new ParsedArticle(title, absoluteUrl, parseDate(contextWindow), context));
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
        if (title.length() < 12 || properties.blockedTitles().contains(lowerTitle) || lowerTitle.matches("\\d+")) {
            return false;
        }
        for (String blocked : properties.blockedLinkParts()) {
            if (lowerHref.contains(blocked)) {
                return false;
            }
        }
        if (isUberCategoryLink(source, lowerHref)) {
            return false;
        }
        boolean pathMatches = config.articlePathSignals().isEmpty()
                || config.articlePathSignals().stream().anyMatch(lowerHref::contains);
        boolean textMatches = config.requiredTextSignals().isEmpty()
                || config.requiredTextSignals().stream().anyMatch(signal ->
                lowerTitle.contains(signal) || block.toLowerCase(Locale.ROOT).contains(signal));
        return pathMatches && textMatches;
    }

    private boolean isUberCategoryLink(BlogSource source, String lowerHref) {
        if (!"uber".equals(source.getSourceKey())) {
            return false;
        }
        return UBER_CATEGORY_PATHS.stream().anyMatch(lowerHref::contains);
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
        return ArticleDateParser.parseListPageDate(value);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
