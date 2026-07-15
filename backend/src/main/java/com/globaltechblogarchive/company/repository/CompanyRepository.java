package com.globaltechblogarchive.company.repository;

import com.globaltechblogarchive.company.domain.Company;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByCompanyKeyIn(Collection<String> companyKeys);

    Optional<Company> findByCompanyKey(String companyKey);

    List<Company> findAllByOrderByCompanyNameAsc();
}
