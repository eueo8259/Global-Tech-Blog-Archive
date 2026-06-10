package com.globaltechblogarchive.article.repository;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @EntityGraph(attributePaths = "company")
    Page<Article> findAllByOrderByPublishedAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "company")
    Page<Article> findByCategoryOrderByPublishedAtDescIdDesc(ArticleCategory category, Pageable pageable);

    boolean existsByCompanyIdAndArticleUrlHash(Long companyId, String articleUrlHash);
}
