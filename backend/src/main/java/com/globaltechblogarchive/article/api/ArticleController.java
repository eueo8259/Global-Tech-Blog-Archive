package com.globaltechblogarchive.article.api;

import com.globaltechblogarchive.article.api.dto.ArticlePageResponse;
import com.globaltechblogarchive.article.application.ArticleService;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping("/api/articles")
    public ArticlePageResponse getArticles(
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) {
            throw new InvalidInputException("page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new InvalidInputException("size must be greater than 0");
        }

        return articleService.getArticles(category, page, size);
    }
}
