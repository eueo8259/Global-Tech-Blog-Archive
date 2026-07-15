package com.globaltechblogarchive.article.repository;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @EntityGraph(attributePaths = "company")
    Page<Article> findAllByOrderByPublishedAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "company")
    Page<Article> findByCategoryOrderByPublishedAtDescIdDesc(ArticleCategory category, Pageable pageable);

    @Query("""
            select article.articleUrlHash
            from Article article
            where article.company.id = :companyId
              and article.articleUrlHash in :hashes
            """)
    Set<String> findExistingHashes(
            @Param("companyId") Long companyId,
            @Param("hashes") Collection<String> hashes
    );

    @EntityGraph(attributePaths = "company")
    List<Article> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "company")
    Page<Article> findByCompany_CompanyKeyOrderByPublishedAtDescIdDesc(String companyKey, Pageable pageable);

    @EntityGraph(attributePaths = "company")
    Page<Article> findByCategoryAndCompany_CompanyKeyOrderByPublishedAtDescIdDesc(
            ArticleCategory articleCategory,
            String companyKey,
            Pageable pageable
    );
}
