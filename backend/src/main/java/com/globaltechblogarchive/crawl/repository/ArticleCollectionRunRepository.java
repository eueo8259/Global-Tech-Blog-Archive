package com.globaltechblogarchive.crawl.repository;

import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleCollectionRunRepository extends JpaRepository<ArticleCollectionRun, Long> {
}
