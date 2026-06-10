package com.globaltechblogarchive.crawl.repository;

import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleDiscoveryLogRepository extends JpaRepository<ArticleDiscoveryLog, Long> {
}
