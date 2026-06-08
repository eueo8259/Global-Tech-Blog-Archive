package com.globaltechblogarchive.source.application;

import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Profile("local")
@Component
@RequiredArgsConstructor
public class BlogSourceSeedInitializer implements ApplicationRunner {

    private final BlogSourceRepository blogSourceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (BlogSource source : sources()) {
            blogSourceRepository.findByCompanyKey(source.getCompanyKey())
                    .ifPresentOrElse(
                            existing -> existing.updateCollectionTarget(
                                    source.getSiteUrl(),
                                    source.getFeedUrl(),
                                    source.getCollectionMethod()
                            ),
                            () -> blogSourceRepository.save(source)
                    );
        }
    }

    private List<BlogSource> sources() {
        return List.of(
                rss("openai", "OpenAI", "https://openai.com/news/", "https://openai.com/news/rss.xml"),
                sitemap("anthropic", "Anthropic", "https://www.anthropic.com/engineering", "https://www.anthropic.com/sitemap.xml"),
                html("claude", "Claude", "https://claude.com/blog"),
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
        return BlogSource.create(companyKey, companyName, siteUrl, null, CollectionMethod.HTML_SCRAPING);
    }

    private BlogSource rss(String companyKey, String companyName, String siteUrl, String feedUrl) {
        return BlogSource.create(companyKey, companyName, siteUrl, feedUrl, CollectionMethod.RSS);
    }

    private BlogSource atom(String companyKey, String companyName, String siteUrl, String feedUrl) {
        return BlogSource.create(companyKey, companyName, siteUrl, feedUrl, CollectionMethod.ATOM);
    }

    private BlogSource sitemap(String companyKey, String companyName, String siteUrl, String feedUrl) {
        return BlogSource.create(companyKey, companyName, siteUrl, feedUrl, CollectionMethod.SITEMAP);
    }

}
