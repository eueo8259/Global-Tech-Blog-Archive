package com.globaltechblogarchive.company.api;

import com.globaltechblogarchive.company.api.dto.CompanyResponse;
import com.globaltechblogarchive.company.application.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/api/companies")
    public List<CompanyResponse> getCompanies(){
        return companyService.getCompanies();
    }
}
