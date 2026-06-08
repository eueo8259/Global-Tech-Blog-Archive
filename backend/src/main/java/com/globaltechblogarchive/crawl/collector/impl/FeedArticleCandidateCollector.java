package com.globaltechblogarchive.crawl.collector.impl;

import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.support.ArticleDateParser;
import com.globaltechblogarchive.crawl.support.ArticleCandidateCollectionPolicy;
import com.globaltechblogarchive.crawl.support.TextCleaner;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.crawl.support.XmlDocumentSupport;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class FeedArticleCandidateCollector implements ArticleCandidateCollector {

    private final SourceDocumentClient fetcher;

    public FeedArticleCandidateCollector(SourceDocumentClient fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public boolean supports(CollectionMethod collectionMethod) {
        return collectionMethod == CollectionMethod.RSS || collectionMethod == CollectionMethod.ATOM;
    }

    @Override
    public List<ParsedArticle> collect(BlogSource source) {
        if (source.getFeedUrl() == null || source.getFeedUrl().isBlank()) {
            throw new IllegalArgumentException("Feed URL is required for " + source.getSourceKey());
        }
        return ArticleCandidateCollectionPolicy.apply(parse(source, fetcher.fetch(source.getFeedUrl())));
    }

    List<ParsedArticle> parse(BlogSource source, String xml) {
        Element root = XmlDocumentSupport.parseRoot(xml, "Feed XML parsing failed");
        if ("feed".equalsIgnoreCase(root.getTagName())) {
            return parseAtom(source, root);
        }
        return parseRss(source, root);
    }

    private List<ParsedArticle> parseRss(BlogSource source, Element root) {
        NodeList items = root.getElementsByTagName("item");
        List<ParsedArticle> cards = new ArrayList<>();
        for (int index = 0; index < items.getLength(); index++) {
            Element item = (Element) items.item(index);
            String title = text(item, "title");
            String link = text(item, "link");
            if (title.isBlank() || link.isBlank()) {
                continue;
            }
            String absoluteUrl = UrlNormalizer.absolute(source.getSiteUrl(), link);
            cards.add(new ParsedArticle(
                    TextCleaner.clean(title),
                    absoluteUrl,
                    ArticleDateParser.parseFeedDate(firstNonBlank(text(item, "pubDate"), text(item, "dc:date"))),
                    TextCleaner.shortContext(text(item, "description"), title)
            ));
        }
        return cards;
    }

    private List<ParsedArticle> parseAtom(BlogSource source, Element root) {
        NodeList entries = root.getElementsByTagName("entry");
        List<ParsedArticle> cards = new ArrayList<>();
        for (int index = 0; index < entries.getLength(); index++) {
            Element entry = (Element) entries.item(index);
            String title = text(entry, "title");
            String link = atomLink(entry);
            if (title.isBlank() || link.isBlank()) {
                continue;
            }
            cards.add(new ParsedArticle(
                    TextCleaner.clean(title),
                    UrlNormalizer.absolute(source.getSiteUrl(), link),
                    ArticleDateParser.parseFeedDate(firstNonBlank(text(entry, "published"), text(entry, "updated"))),
                    TextCleaner.shortContext(firstNonBlank(text(entry, "summary"), text(entry, "content")), title)
            ));
        }
        return cards;
    }

    private String text(Element element, String tagName) {
        return XmlDocumentSupport.text(element, tagName);
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

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
