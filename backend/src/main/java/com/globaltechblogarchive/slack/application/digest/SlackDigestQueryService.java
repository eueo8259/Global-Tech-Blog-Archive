package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.repository.SlackDigestArticleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlackDigestQueryService {

    private final SlackDigestArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public boolean hasArticles(
            Long channelId,
            LocalDateTime windowStartedAt,
            LocalDateTime windowEndedAt
    ) {
        return articleRepository.existsDigestArticles(
                channelId,
                windowStartedAt,
                windowEndedAt
        );
    }

    @Transactional(readOnly = true)
    public List<SlackDigestArticle> findArticles(
            Long channelId,
            LocalDateTime windowStartedAt,
            LocalDateTime windowEndedAt
    ) {
        return articleRepository.findDigestArticles(channelId, windowStartedAt, windowEndedAt)
                .stream()
                .map(article -> new SlackDigestArticle(
                        article.getArticleId(),
                        article.getCompanyId(),
                        article.getCompanyName(),
                        article.getTitle(),
                        article.getArticleUrl(),
                        article.getPublishedAt()
                ))
                .toList();
    }
}
