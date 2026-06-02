package com.globaltechblogarchive.source;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogSourceRepository extends JpaRepository<BlogSource, Long> {

    List<BlogSource> findByEnabledTrue();
}
