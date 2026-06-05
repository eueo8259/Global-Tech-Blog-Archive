package com.globaltechblogarchive.collection.repository;

import com.globaltechblogarchive.collection.domain.ArticleCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleCollectionItemRepository extends JpaRepository<ArticleCollectionItem, Long> {

    long countByRunId(Long runId);
}
