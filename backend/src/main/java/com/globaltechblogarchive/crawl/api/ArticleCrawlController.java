package com.globaltechblogarchive.crawl.api;

import com.globaltechblogarchive.crawl.api.dto.ArticleCrawlResponse;
import com.globaltechblogarchive.crawl.api.dto.InitialArticleCrawlResponse;
import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/api/admin/article-crawls/initial-run")
    public InitialArticleCrawlResponse runInitial() {
        return InitialArticleCrawlResponse.from(articleCrawlService.runInitial());
    }

    @PostMapping("/api/admin/article-crawls/sources/{sourceKey}/run")
    public ArticleCrawlResponse runSource(@PathVariable String sourceKey) {
        return ArticleCrawlResponse.from(articleCrawlService.runSource(sourceKey));
    }

    @PostMapping("/api/admin/article-crawls/sources/{sourceKey}/initial-run")
    public InitialArticleCrawlResponse runSourceInitial(@PathVariable String sourceKey) {
        return InitialArticleCrawlResponse.from(articleCrawlService.runSourceInitial(sourceKey));
    }
}
