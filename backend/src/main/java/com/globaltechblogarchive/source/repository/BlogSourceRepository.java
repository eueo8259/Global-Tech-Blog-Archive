package com.globaltechblogarchive.source.repository;

import com.globaltechblogarchive.source.domain.BlogSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogSourceRepository extends JpaRepository<BlogSource, Long> {

    @EntityGraph(attributePaths = "company")
    List<BlogSource> findByEnabledTrue();

    @EntityGraph(attributePaths = "company")
    Optional<BlogSource> findWithCompanyById(Long id);
}
