package com.globaltechblogarchive.collection.collector;

import com.globaltechblogarchive.collection.parser.ParsedArticleCard;
import com.globaltechblogarchive.collection.support.TextCleaner;
import com.globaltechblogarchive.collection.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class FeedArticleCandidateCollector implements ArticleCandidateCollector {

    private final SourceDocumentFetcher fetcher;

    public FeedArticleCandidateCollector(SourceDocumentFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public boolean supports(CollectionMethod collectionMethod) {
        return collectionMethod == CollectionMethod.RSS || collectionMethod == CollectionMethod.ATOM;
    }

    @Override
    public List<ParsedArticleCard> collect(BlogSource source) {
        if (source.getFeedUrl() == null || source.getFeedUrl().isBlank()) {
            throw new IllegalArgumentException("Feed URL is required for " + source.getCompanyKey());
        }
        return parse(source, fetcher.fetch(source.getFeedUrl()));
    }

    List<ParsedArticleCard> parse(BlogSource source, String xml) {
        Element root = parseRoot(xml);
        if ("feed".equalsIgnoreCase(root.getTagName())) {
            return parseAtom(source, root);
        }
        return parseRss(source, root);
    }

    private List<ParsedArticleCard> parseRss(BlogSource source, Element root) {
        NodeList items = root.getElementsByTagName("item");
        List<ParsedArticleCard> cards = new ArrayList<>();
        for (int index = 0; index < items.getLength(); index++) {
            Element item = (Element) items.item(index);
            String title = text(item, "title");
            String link = text(item, "link");
            if (title.isBlank() || link.isBlank()) {
                continue;
            }
            String absoluteUrl = UrlNormalizer.absolute(source.getSiteUrl(), link);
            cards.add(new ParsedArticleCard(
                    TextCleaner.clean(title),
                    absoluteUrl,
                    parseDate(firstNonBlank(text(item, "pubDate"), text(item, "dc:date"))),
                    TextCleaner.shortContext(text(item, "description"), title)
            ));
        }
        return cards;
    }

    private List<ParsedArticleCard> parseAtom(BlogSource source, Element root) {
        NodeList entries = root.getElementsByTagName("entry");
        List<ParsedArticleCard> cards = new ArrayList<>();
        for (int index = 0; index < entries.getLength(); index++) {
            Element entry = (Element) entries.item(index);
            String title = text(entry, "title");
            String link = atomLink(entry);
            if (title.isBlank() || link.isBlank()) {
                continue;
            }
            cards.add(new ParsedArticleCard(
                    TextCleaner.clean(title),
                    UrlNormalizer.absolute(source.getSiteUrl(), link),
                    parseDate(firstNonBlank(text(entry, "published"), text(entry, "updated"))),
                    TextCleaner.shortContext(firstNonBlank(text(entry, "summary"), text(entry, "content")), title)
            ));
        }
        return cards;
    }

    private Element parseRoot(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)))
                    .getDocumentElement();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Feed XML parsing failed", exception);
        }
    }

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        String value = node.getTextContent();
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String atomLink(Element entry) {
        NodeList links = entry.getElementsByTagName("link");
        for (int index = 0; index < links.getLength(); index++) {
            Element link = (Element) links.item(index);
            String rel = link.getAttribute("rel");
            if (rel.isBlank() || "alternate".equals(rel)) {
                String href = link.getAttribute("href");
                if (!href.isBlank()) {
                    return href;
                }
            }
        }
        return "";
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.RFC_1123_DATE_TIME,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_ZONED_DATE_TIME
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return ZonedDateTime.parse(value, formatter).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                try {
                    return OffsetDateTime.parse(value, formatter).toLocalDateTime();
                } catch (DateTimeParseException ignoredAgain) {
                    // Try the next feed date format.
                }
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
}
