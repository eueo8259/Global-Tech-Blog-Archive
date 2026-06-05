package com.globaltechblogarchive.collection.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.collection.parser.ParsedArticleCard;
import com.globaltechblogarchive.collection.support.TextCleaner;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WordPressRestArticleCandidateCollector implements ArticleCandidateCollector {

    private final SourceDocumentFetcher fetcher;
    private final ObjectMapper objectMapper;

    public WordPressRestArticleCandidateCollector(SourceDocumentFetcher fetcher, ObjectMapper objectMapper) {
        this.fetcher = fetcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(CollectionMethod collectionMethod) {
        return collectionMethod == CollectionMethod.WORDPRESS_REST;
    }

    @Override
    public List<ParsedArticleCard> collect(BlogSource source) {
        if (source.getFeedUrl() == null || source.getFeedUrl().isBlank()) {
            throw new IllegalArgumentException("WordPress REST URL is required for " + source.getCompanyKey());
        }
        return parse(fetcher.fetch(source.getFeedUrl()));
    }

    List<ParsedArticleCard> parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                throw new IllegalArgumentException("WordPress REST response must be an array");
            }
            List<ParsedArticleCard> cards = new ArrayList<>();
            for (JsonNode post : root) {
                String title = TextCleaner.clean(post.path("title").path("rendered").asText(""));
                String link = post.path("link").asText("");
                if (title.isBlank() || link.isBlank()) {
                    continue;
                }
                cards.add(new ParsedArticleCard(
                        title,
                        link,
                        parseDate(post.path("date").asText("")),
                        TextCleaner.shortContext(post.path("excerpt").path("rendered").asText(""), title)
                ));
            }
            return cards;
        } catch (Exception exception) {
            throw new IllegalArgumentException("WordPress REST JSON parsing failed", exception);
        }
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
