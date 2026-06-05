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
                html("openai", "OpenAI", "https://openai.com/news/"),
                html("anthropic", "Anthropic", "https://www.anthropic.com/news"),
                rss("netflix", "Netflix", "https://netflixtechblog.com/", "https://netflixtechblog.com/feed"),
                html("figma", "Figma", "https://www.figma.com/blog/engineering/"),
                rss("meta", "Meta", "https://engineering.fb.com/", "https://engineering.fb.com/feed/"),
                html("uber", "Uber", "https://www.uber.com/blog/engineering"),
                html("airbnb", "Airbnb", "https://airbnb.tech/"),
                html("stripe", "Stripe", "https://stripe.com/blog/engineering"),
                html("cloudflare", "Cloudflare", "https://blog.cloudflare.com/"),
                rss("github", "GitHub", "https://github.blog/engineering/", "https://github.blog/engineering/feed/"),
                html("linkedin", "LinkedIn", "https://engineering.linkedin.com/content/engineering/en-us/blog"),
                wordpressRest("doordash", "DoorDash", "https://careersatdoordash.com/", "https://careersatdoordash.com/wp-json/wp/v2/posts?per_page=20&categories=8"),
                html("discord", "Discord", "https://discord.com/category/engineering"),
                html("shopify", "Shopify", "https://shopify.engineering/"),
                html("datadog", "Datadog", "https://www.datadoghq.com/blog/engineering/"),
                rss("slack", "Slack", "https://slack.engineering/", "https://slack.engineering/feed/"),
                html("amazon-science", "Amazon Science", "https://www.amazon.science/blog")
        );
    }

    private BlogSource html(String companyKey, String companyName, String siteUrl) {
        return BlogSource.create(companyKey, companyName, siteUrl, null, CollectionMethod.HTML_SCRAPING);
    }

    private BlogSource rss(String companyKey, String companyName, String siteUrl, String feedUrl) {
        return BlogSource.create(companyKey, companyName, siteUrl, feedUrl, CollectionMethod.RSS);
    }

    private BlogSource wordpressRest(String companyKey, String companyName, String siteUrl, String feedUrl) {
        return BlogSource.create(companyKey, companyName, siteUrl, feedUrl, CollectionMethod.WORDPRESS_REST);
    }
}
