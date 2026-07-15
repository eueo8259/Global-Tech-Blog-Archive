package com.globaltechblogarchive.slack.application.digest;

import java.time.LocalDateTime;

public record SlackDigestArticle(
        Long articleId,
        Long companyId,
        String companyName,
        String title,
        String articleUrl,
        LocalDateTime publishedAt
) {
}
