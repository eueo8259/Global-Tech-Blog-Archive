package com.globaltechblogarchive.company.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.company.api.dto.CompanyResponse;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.company.repository.CompanyRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void getCompaniesReturnsNameAndKeyInCompanyNameOrder() {
        when(companyRepository.findAllByOrderByCompanyNameAsc()).thenReturn(List.of(
                Company.create("anthropic", "Anthropic"),
                Company.create("openai", "OpenAI")
        ));

        List<CompanyResponse> companies = companyService.getCompanies();

        assertThat(companies).containsExactly(
                new CompanyResponse("Anthropic", "anthropic"),
                new CompanyResponse("OpenAI", "openai")
        );
        verify(companyRepository).findAllByOrderByCompanyNameAsc();
    }
}
