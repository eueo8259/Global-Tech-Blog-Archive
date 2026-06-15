package com.globaltechblogarchive.bootstrap.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.bootstrap.domain.BootstrapAiDecisionRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArchive;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArticleRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapManifest;
import com.globaltechblogarchive.bootstrap.exception.BootstrapArchiveException;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BootstrapImportWriterTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleAiDecisionRepository decisionRepository;

    @Mock
    private CompanyRepository companyRepository;

    private BootstrapImportWriter writer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        writer = new BootstrapImportWriter(articleRepository, decisionRepository, companyRepository);
    }

    @Test
    void importArchiveRejectsNonEmptyContentDatabase() {
        when(articleRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> writer.importArchive(archive()))
                .isInstanceOf(BootstrapArchiveException.class)
                .hasMessageContaining("requires empty");

        verify(companyRepository, never()).findByCompanyKeyIn(anyCollection());
    }

    @Test
    void importArchiveRejectsUnknownCompanyKey() {
        when(articleRepository.count()).thenReturn(0L);
        when(decisionRepository.count()).thenReturn(0L);
        when(companyRepository.findByCompanyKeyIn(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> writer.importArchive(archive()))
                .isInstanceOf(BootstrapArchiveException.class)
                .hasMessageContaining("openai");
    }

    @Test
    @SuppressWarnings("unchecked")
    void importArchiveRestoresOriginalTimestamps() {
        Company company = Company.create("openai", "OpenAI");
        when(articleRepository.count()).thenReturn(0L);
        when(decisionRepository.count()).thenReturn(0L);
        when(companyRepository.findByCompanyKeyIn(anyCollection())).thenReturn(List.of(company));

        writer.importArchive(archive());

        ArgumentCaptor<List<Article>> articleCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<ArticleAiDecision>> decisionCaptor = ArgumentCaptor.forClass(List.class);
        verify(articleRepository).saveAllAndFlush(articleCaptor.capture());
        verify(decisionRepository).saveAllAndFlush(decisionCaptor.capture());

        LocalDateTime expected = LocalDateTime.of(2026, 6, 15, 10, 30);
        assertThat(articleCaptor.getValue().getFirst().getCreatedAt()).isEqualTo(expected);
        assertThat(articleCaptor.getValue().getFirst().getUpdatedAt()).isEqualTo(expected);
        assertThat(decisionCaptor.getValue().getFirst().getCreatedAt()).isEqualTo(expected);
        assertThat(decisionCaptor.getValue().getFirst().getUpdatedAt()).isEqualTo(expected);
    }

    private BootstrapArchive archive() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 15, 10, 30);
        return new BootstrapArchive(
                new BootstrapManifest("MANIFEST", 1, time, 1, 1),
                List.of(new BootstrapArticleRecord(
                        "ARTICLE",
                        "openai",
                        "Translated title",
                        "https://openai.com/news/article",
                        "article-hash",
                        ArticleCategory.AI,
                        time,
                        time,
                        time
                )),
                List.of(new BootstrapAiDecisionRecord(
                        "AI_DECISION",
                        "openai",
                        "article-hash",
                        "https://openai.com/news/article",
                        "Original title",
                        "Translated title",
                        ArticleCategory.AI,
                        true,
                        "gpt-5-mini",
                        "v1",
                        time,
                        time
                ))
        );
    }
}
