package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.collector.impl.FeedArticleCandidateCollector;
import com.globaltechblogarchive.crawl.collector.impl.HtmlArticleCandidateCollector;
import com.globaltechblogarchive.crawl.collector.impl.SitemapArticleCandidateCollector;
import com.globaltechblogarchive.crawl.collector.impl.WordPressRestArticleCandidateCollector;
import com.globaltechblogarchive.crawl.helper.ArticleListParserPropertiesFixture;
import com.globaltechblogarchive.crawl.parser.ArticleListParserRegistry;
import com.globaltechblogarchive.crawl.parser.HtmlArticleListParser;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("real")
class RealArticleSourceCollectionCountTest {

    @Test
    void collectAllRealSourcesAndWriteCountReport() throws IOException {
        SourceDocumentClient fetcher = new SourceDocumentClient();
        List<ArticleCandidateCollector> collectors = List.of(
                new FeedArticleCandidateCollector(fetcher),
                new SitemapArticleCandidateCollector(fetcher),
                new HtmlArticleCandidateCollector(
                        fetcher,
                        new ArticleListParserRegistry(List.of(
                                new HtmlArticleListParser(ArticleListParserPropertiesFixture.full())
                        ))
                ),
                new WordPressRestArticleCandidateCollector(fetcher, new ObjectMapper())
        );

        List<SourceCount> counts = sources().stream()
                .map(source -> collectCount(source, collectors))
                .sorted(Comparator.comparing(SourceCount::sourceKey))
                .toList();

        Path reportPath = Path.of("build", "reports", "real-source-counts.tsv");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, toTsv(counts));

        assertThat(counts).isNotEmpty();
        assertThat(counts)
                .allSatisfy(count -> assertThat(count.count())
                        .as(count.sourceKey() + " candidate count should never be negative")
                        .isGreaterThanOrEqualTo(0));
    }

    private SourceCount collectCount(BlogSource source, List<ArticleCandidateCollector> collectors) {
        ArticleCandidateCollector collector = collectors.stream()
                .filter(candidateCollector -> candidateCollector.supports(source.getCollectionMethod()))
                .findFirst()
                .orElseThrow();
        try {
            int count = collector.collect(source).size();
            return new SourceCount(
                    source.getCompany().getCompanyKey(),
                    source.getCompany().getCompanyName(),
                    source.getSourceKey(),
                    source.getSourceName(),
                    source.getCollectionMethod(),
                    count,
                    ""
            );
        } catch (Exception exception) {
            return new SourceCount(
                    source.getCompany().getCompanyKey(),
                    source.getCompany().getCompanyName(),
                    source.getSourceKey(),
                    source.getSourceName(),
                    source.getCollectionMethod(),
                    0,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        }
    }

    private String toTsv(List<SourceCount> counts) {
        StringBuilder builder = new StringBuilder("companyKey\tcompanyName\tsourceKey\tsourceName\tmethod\tcandidateCount\terror\n");
        for (SourceCount count : counts) {
            builder.append(count.companyKey()).append('\t')
                    .append(count.companyName()).append('\t')
                    .append(count.sourceKey()).append('\t')
                    .append(count.sourceName()).append('\t')
                    .append(count.method()).append('\t')
                    .append(count.count()).append('\t')
                    .append(count.error().replace('\t', ' ').replace('\n', ' '))
                    .append('\n');
        }
        return builder.toString();
    }

    private List<BlogSource> sources() {
        return List.of(
                rss("openai", "OpenAI", "https://openai.com/news/", "https://openai.com/news/rss.xml"),
                sitemap("anthropic", "Anthropic", "anthropic-engineering", "Anthropic Engineering", "https://www.anthropic.com/engineering", "https://www.anthropic.com/sitemap.xml"),
                html("anthropic", "Anthropic", "claude-blog", "Claude Blog", "https://claude.com/blog"),
                rss("netflix", "Netflix", "https://netflixtechblog.com/", "https://netflixtechblog.com/feed"),
                atom("figma", "Figma", "https://www.figma.com/blog/engineering/", "https://www.figma.com/blog/feed/atom.xml"),
                rss("meta", "Meta", "https://engineering.fb.com/", "https://engineering.fb.com/feed/"),
                html("uber", "Uber", "https://www.uber.com/blog/engineering"),
                rss("airbnb", "Airbnb", "https://medium.com/airbnb-engineering", "https://medium.com/feed/airbnb-engineering"),
                rss("pinterest", "Pinterest", "https://medium.com/pinterest-engineering", "https://medium.com/feed/pinterest-engineering"),
                rss("stripe", "Stripe", "https://stripe.com/blog/engineering", "https://stripe.com/blog/feed.rss"),
                rss("cloudflare", "Cloudflare", "https://blog.cloudflare.com/", "https://blog.cloudflare.com/tag/engineering/rss/"),
                rss("github", "GitHub", "https://github.blog/engineering/", "https://github.blog/engineering/feed/"),
                html("linkedin", "LinkedIn", "https://engineering.linkedin.com/content/engineering/en-us/blog"),
                html("discord", "Discord", "https://discord.com/category/engineering"),
                sitemap("shopify", "Shopify", "https://shopify.engineering/", "https://shopify.engineering/sitemap.xml"),
                rss("datadog", "Datadog", "https://www.datadoghq.com/blog/engineering/", "https://www.datadoghq.com/blog/engineering/index.xml"),
                rss("slack", "Slack", "https://slack.engineering/", "https://slack.engineering/feed/"),
                rss("amazon-science", "Amazon Science", "https://www.amazon.science/blog", "https://www.amazon.science/index.rss")
        );
    }

    private BlogSource html(String companyKey, String companyName, String siteUrl) {
        return html(companyKey, companyName, companyKey, companyName, siteUrl);
    }

    private BlogSource rss(String companyKey, String companyName, String siteUrl, String feedUrl) {
        return BlogSource.create(Company.create(companyKey, companyName), companyKey, companyName, siteUrl, feedUrl, CollectionMethod.RSS);
    }

    private BlogSource atom(String companyKey, String companyName, String siteUrl, String feedUrl) {
        return BlogSource.create(Company.create(companyKey, companyName), companyKey, companyName, siteUrl, feedUrl, CollectionMethod.ATOM);
    }

    private BlogSource sitemap(String companyKey, String companyName, String siteUrl, String feedUrl) {
        return sitemap(companyKey, companyName, companyKey, companyName, siteUrl, feedUrl);
    }

    private BlogSource html(String companyKey, String companyName, String sourceKey, String sourceName, String siteUrl) {
        return BlogSource.create(Company.create(companyKey, companyName), sourceKey, sourceName, siteUrl, null, CollectionMethod.HTML_SCRAPING);
    }

    private BlogSource sitemap(String companyKey, String companyName, String sourceKey, String sourceName, String siteUrl, String feedUrl) {
        return BlogSource.create(Company.create(companyKey, companyName), sourceKey, sourceName, siteUrl, feedUrl, CollectionMethod.SITEMAP);
    }

    private record SourceCount(
            String companyKey,
            String companyName,
            String sourceKey,
            String sourceName,
            CollectionMethod method,
            int count,
            String error
    ) {
    }
}