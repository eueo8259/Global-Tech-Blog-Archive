package com.globaltechblogarchive.bootstrap.application;

import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.bootstrap.domain.BootstrapAiDecisionRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArchive;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArticleRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapManifest;
import com.globaltechblogarchive.bootstrap.infrastructure.BootstrapArchiveFileStore;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BootstrapExportService {

    private final ArticleRepository articleRepository;
    private final ArticleAiDecisionRepository decisionRepository;
    private final BootstrapArchiveFileStore fileStore;

    @Transactional(readOnly = true)
    public BootstrapResult exportTo(Path target) {
        List<BootstrapArticleRecord> articles = articleRepository.findAllByOrderByIdAsc().stream()
                .map(BootstrapArticleRecord::from)
                .toList();
        List<BootstrapAiDecisionRecord> decisions = decisionRepository.findAllByOrderByIdAsc().stream()
                .map(BootstrapAiDecisionRecord::from)
                .toList();
        BootstrapArchive archive = new BootstrapArchive(
                BootstrapManifest.create(articles.size(), decisions.size()),
                articles,
                decisions
        );
        String checksum = fileStore.write(target, archive);
        return new BootstrapResult(articles.size(), decisions.size(), checksum, target);
    }
}
