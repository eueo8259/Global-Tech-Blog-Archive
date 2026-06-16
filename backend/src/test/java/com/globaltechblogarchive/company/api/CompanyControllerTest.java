package com.globaltechblogarchive.company.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.company.api.dto.CompanyResponse;
import com.globaltechblogarchive.company.application.CompanyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanyControllerTest {

    private CompanyService companyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        companyService = mock(CompanyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CompanyController(companyService)).build();
    }

    @Test
    void getCompaniesReturnsCompanyNameAndCompanyKey() throws Exception {
        when(companyService.getCompanies()).thenReturn(List.of(
                new CompanyResponse("Anthropic", "anthropic"),
                new CompanyResponse("OpenAI", "openai")
        ));

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Anthropic"))
                .andExpect(jsonPath("$[0].companyKey").value("anthropic"))
                .andExpect(jsonPath("$[1].companyName").value("OpenAI"))
                .andExpect(jsonPath("$[1].companyKey").value("openai"));
    }
}
