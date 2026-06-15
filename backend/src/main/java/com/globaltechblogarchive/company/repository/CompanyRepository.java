package com.globaltechblogarchive.company.repository;

import com.globaltechblogarchive.company.domain.Company;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByCompanyKeyIn(Collection<String> companyKeys);

    List<Company> findAllByOrderByCompanyNameAsc();
}
