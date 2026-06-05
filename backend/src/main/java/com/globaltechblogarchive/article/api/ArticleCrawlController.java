package com.globaltechblogarchive.article.api;

import com.globaltechblogarchive.article.api.dto.crawl.ArticleCrawlResponse;
import com.globaltechblogarchive.collection.application.ArticleCrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequiredArgsConstructor
public class ArticleCrawlController {

    private final ArticleCrawlService articleCrawlService;

    @PostMapping("/api/admin/article-crawls/run")
    public ArticleCrawlResponse run() {
        return ArticleCrawlResponse.from(articleCrawlService.run());
    }
}
