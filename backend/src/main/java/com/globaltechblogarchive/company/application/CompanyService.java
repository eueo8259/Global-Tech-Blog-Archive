package com.globaltechblogarchive.company.application;

import com.globaltechblogarchive.company.api.dto.CompanyResponse;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<CompanyResponse> getCompanies() {
        return companyRepository.findAllByOrderByCompanyNameAsc()
                .stream()
                .map(company -> new CompanyResponse(
                        company.getCompanyKey(),
                        company.getCompanyName()
                ))
                .toList();
    }
}
