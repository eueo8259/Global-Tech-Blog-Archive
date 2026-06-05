package com.globaltechblogarchive.collection.support;

import com.globaltechblogarchive.collection.parser.ParsedArticleCard;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CandidateValidationWarnings {

    private static final List<String> NON_ARTICLE_PATH_PARTS = List.of(
            "/tag/",
            "/tags/",
            "/category/",
            "/categories/",
            "/author/",
            "/authors/",
            "/topic/",
            "/topics/",
            "/search",
            "/feed",
            "/rss",
            "/page/"
    );
    private static final List<String> GENERIC_TITLES = List.of(
            "blog",
            "engineering",
            "ai",
            "security",
            "database",
            "infrastructure",
            "read more",
            "visit link"
    );

    private CandidateValidationWarnings() {
    }

    public static List<String> from(BlogSource source, ParsedArticleCard card, String normalizedUrl) {
        List<String> warnings = new ArrayList<>();
        String title = TextCleaner.clean(card.originalTitle());
        String context = TextCleaner.clean(card.shortContext());
        String path = path(normalizedUrl).toLowerCase(Locale.ROOT);

        if (title.length() < 12 || GENERIC_TITLES.contains(title.toLowerCase(Locale.ROOT))) {
            warnings.add("SUSPICIOUS_TITLE");
        }
        if (context.isBlank() || context.equals(title)) {
            warnings.add("CONTEXT_FALLBACK_ONLY");
        }
        if (card.publishedAt() == null) {
            warnings.add("PUBLISHED_AT_MISSING");
        }
        if (path.equals("/") || path.isBlank()) {
            warnings.add("ROOT_URL");
        }
        for (String nonArticlePathPart : NON_ARTICLE_PATH_PARTS) {
            if (path.contains(nonArticlePathPart)) {
                warnings.add("NON_ARTICLE_PATH");
                break;
            }
        }
        if (!isExpectedHost(source, normalizedUrl)) {
            warnings.add("UNEXPECTED_HOST");
        }
        return warnings;
    }

    private static String path(String url) {
        return URI.create(url).getPath();
    }

    private static boolean isExpectedHost(BlogSource source, String normalizedUrl) {
        String candidateHost = URI.create(normalizedUrl).getHost();
        String sourceHost = URI.create(source.getSiteUrl()).getHost();
        if (candidateHost == null || sourceHost == null) {
            return false;
        }
        if (candidateHost.equalsIgnoreCase(sourceHost)) {
            return true;
        }
        return "airbnb".equals(source.getCompanyKey())
                && "medium.com".equalsIgnoreCase(candidateHost);
    }
}
