package com.globaltechblogarchive.bootstrap.application;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.bootstrap.domain.BootstrapAiDecisionRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArchive;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArticleRecord;
import com.globaltechblogarchive.bootstrap.exception.BootstrapArchiveException;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BootstrapImportWriter {

    private final ArticleRepository articleRepository;
    private final ArticleAiDecisionRepository decisionRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public void importArchive(BootstrapArchive archive) {
        if (articleRepository.count() != 0 || decisionRepository.count() != 0) {
            throw new BootstrapArchiveException(
                    "Bootstrap import requires empty articles and article_ai_decisions tables"
            );
        }

        Set<String> companyKeys = archive.articles().stream()
                .map(BootstrapArticleRecord::companyKey)
                .collect(Collectors.toSet());
        archive.aiDecisions().stream()
                .map(BootstrapAiDecisionRecord::companyKey)
                .forEach(companyKeys::add);

        Map<String, Company> companies = companyRepository.findByCompanyKeyIn(companyKeys).stream()
                .collect(Collectors.toMap(Company::getCompanyKey, Function.identity()));
        if (companies.size() != companyKeys.size()) {
            Set<String> missingKeys = companyKeys.stream()
                    .filter(key -> !companies.containsKey(key))
                    .collect(Collectors.toSet());
            throw new BootstrapArchiveException("Unknown bootstrap company keys: " + missingKeys);
        }

        articleRepository.saveAllAndFlush(archive.articles().stream()
                .map(record -> restoreArticle(record, companies.get(record.companyKey())))
                .toList());
        decisionRepository.saveAllAndFlush(archive.aiDecisions().stream()
                .map(record -> restoreDecision(record, companies.get(record.companyKey())))
                .toList());
    }

    private Article restoreArticle(BootstrapArticleRecord record, Company company) {
        return Article.restore(
                company,
                record.title(),
                record.articleUrl(),
                record.articleUrlHash(),
                record.category(),
                record.publishedAt(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private ArticleAiDecision restoreDecision(BootstrapAiDecisionRecord record, Company company) {
        return ArticleAiDecision.restore(
                company,
                record.articleUrlHash(),
                record.articleUrl(),
                record.originalTitle(),
                record.translatedTitle(),
                record.category(),
                record.saveTarget(),
                record.model(),
                record.promptVersion(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
