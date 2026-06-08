package com.globaltechblogarchive.source.repository;

import java.util.List;
import com.globaltechblogarchive.source.domain.BlogSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogSourceRepository extends JpaRepository<BlogSource, Long> {

    List<BlogSource> findByEnabledTrue();
}
