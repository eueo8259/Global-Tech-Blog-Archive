package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.application.ArticleCandidateFactory;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.support.UrlHash;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleCandidateFactoryTest {

    @Mock
    private ArticleRepository articleRepository;

    @Test
    void createLoadsExistingHashesOnceAndMarksDuplicates() {
        BlogSource source = source();
        ParsedArticle existing = article("existing");
        ParsedArticle newArticle = article("new");
        String existingHash = hash(existing);
        String newHash = hash(newArticle);
        when(articleRepository.findExistingHashes(1L, List.of(existingHash, newHash)))
                .thenReturn(Set.of(existingHash));

        List<ArticleCandidate> candidates = new ArticleCandidateFactory(articleRepository)
                .create(source, List.of(existing, newArticle), Map.of());

        assertThat(candidates).extracting(ArticleCandidate::duplicate)
                .containsExactly(true, false);
        verify(articleRepository).findExistingHashes(1L, List.of(existingHash, newHash));
    }

    private BlogSource source() {
        Company company = Company.create("test-company", "Test Company");
        ReflectionTestUtils.setField(company, "id", 1L);
        return BlogSource.create(
                company,
                "test-source",
                "Test Source",
                "https://example.com",
                "https://example.com/feed",
                CollectionMethod.RSS
        );
    }

    private ParsedArticle article(String slug) {
        return new ParsedArticle(
                "Article " + slug,
                "https://example.com/articles/" + slug,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                "Context " + slug
        );
    }

    private String hash(ParsedArticle article) {
        return UrlHash.sha256(UrlNormalizer.normalize(article.originalUrl()));
    }
}
