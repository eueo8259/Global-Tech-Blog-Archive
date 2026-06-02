package com.globaltechblogarchive.article.repository;

import com.globaltechblogarchive.article.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
