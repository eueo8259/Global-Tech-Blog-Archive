package com.globaltechblogarchive.crawl.collector.impl;

import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.support.ArticleDateParser;
import com.globaltechblogarchive.crawl.support.ArticleCandidateCollectionPolicy;
import com.globaltechblogarchive.crawl.support.HtmlMetadataExtractor;
import com.globaltechblogarchive.crawl.support.TextCleaner;
import com.globaltechblogarchive.crawl.support.XmlDocumentSupport;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
@RequiredArgsConstructor
public class SitemapArticleCandidateCollector implements ArticleCandidateCollector {

    private final SourceDocumentClient fetcher;

    @Override
    public boolean supports(CollectionMethod collectionMethod) {
        return collectionMethod == CollectionMethod.SITEMAP;
    }

    @Override
    public List<ParsedArticle> collect(BlogSource source, CrawlMode mode) {
        String sitemapUrl = source.getFeedUrl();
        if (sitemapUrl == null || sitemapUrl.isBlank()) {
            sitemapUrl = source.getSiteUrl().replaceAll("/+$", "") + "/sitemap.xml";
        }
        List<SitemapEntry> entries = parse(source, fetcher.fetch(sitemapUrl)).stream()
                .filter(entry -> isArticleUrl(source, entry.location()))
                .toList();
        List<ParsedArticle> cards = ArticleCandidateCollectionPolicy.select(entries, mode, SitemapEntry::lastModified).stream()
                .map(entry -> toCard(source, entry))
                .filter(card -> !card.originalTitle().isBlank())
                .toList();
        return ArticleCandidateCollectionPolicy.apply(cards, mode);
    }

    List<SitemapEntry> parse(BlogSource source, String xml) {
        Element root = XmlDocumentSupport.parseRoot(xml, "Sitemap XML parsing failed");
        NodeList urls = root.getElementsByTagName("url");
        return java.util.stream.IntStream.range(0, urls.getLength())
                .mapToObj(index -> (Element) urls.item(index))
                .map(url -> new SitemapEntry(
                        text(url, "loc"),
                        ArticleDateParser.parseSitemapDate(text(url, "lastmod"))
                ))
                .filter(entry -> !entry.location().isBlank())
                .toList();
    }

    private ParsedArticle toCard(BlogSource source, SitemapEntry entry) {
        String html = fetcher.fetch(entry.location());
        String title = firstNonBlank(
                HtmlMetadataExtractor.metaContent(html, "og:title"),
                HtmlMetadataExtractor.metaContent(html, "twitter:title"),
                HtmlMetadataExtractor.titleTag(html)
        );
        String description = firstNonBlank(
                HtmlMetadataExtractor.metaContent(html, "description"),
                HtmlMetadataExtractor.metaContent(html, "og:description"),
                title
        );
        LocalDateTime publishedAt = firstNonNull(
                ArticleDateParser.parseSitemapDate(firstNonBlank(
                        HtmlMetadataExtractor.metaContent(html, "article:published_time"),
                        HtmlMetadataExtractor.metaContent(html, "datePublished"),
                        HtmlMetadataExtractor.datePublished(html)
                )),
                entry.lastModified()
        );
        return new ParsedArticle(
                cleanTitle(title, source.getCompany().getCompanyName()),
                entry.location(),
                publishedAt,
                TextCleaner.shortContext(description, title)
        );
    }

    private boolean isArticleUrl(BlogSource source, String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if ("anthropic-engineering".equals(source.getSourceKey())) {
            return lower.contains("anthropic.com/engineering/") && !lower.endsWith("/engineering/");
        }
        if ("shopify".equals(source.getSourceKey())) {
            return lower.matches("https://shopify\\.engineering/[^/?#]+/?");
        }
        return lower.startsWith(source.getSiteUrl().toLowerCase(Locale.ROOT));
    }

    private String text(Element element, String tagName) {
        return XmlDocumentSupport.text(element, tagName);
    }

    private String cleanTitle(String title, String companyName) {
        String cleaned = TextCleaner.clean(title);
        return cleaned
                .replace(" \\ " + companyName, "")
                .replace(" - " + companyName, "")
                .trim();
    }

    private String firstNonBlank(String first, String second, String third) {
        return firstNonBlank(firstNonBlank(first, second), third);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second == null) {
            return "";
        }
        return second;
    }

    private LocalDateTime firstNonNull(LocalDateTime first, LocalDateTime second) {
        if (first != null) {
            return first;
        }
        return second;
    }

    record SitemapEntry(String location, LocalDateTime lastModified) {
    }
}
