package com.globaltechblogarchive.crawl.repository;

import com.globaltechblogarchive.crawl.domain.ArticleCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleCollectionItemRepository extends JpaRepository<ArticleCollectionItem, Long> {

    long countByRunId(Long runId);
}
