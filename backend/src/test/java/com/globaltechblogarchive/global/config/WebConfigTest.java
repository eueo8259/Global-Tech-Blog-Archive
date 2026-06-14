package com.globaltechblogarchive.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.article.api.ArticleController;
import com.globaltechblogarchive.article.application.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleController.class)
@Import(WebConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=https://techport.example,http://localhost:5173")
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @Test
    void preflightRequestAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/articles")
                        .header("Origin", "https://techport.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://techport.example"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,OPTIONS"));
    }

    @Test
    void preflightRequestRejectsUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/articles")
                        .header("Origin", "https://unknown.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
