package com.globaltechblogarchive.slack.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.global.error.GlobalExceptionHandler;
import com.globaltechblogarchive.slack.application.SlackTestDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SlackTestDeliveryControllerTest {

    private SlackTestDeliveryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(SlackTestDeliveryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SlackTestDeliveryController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void sendReturnsDeliveredChannelCount() throws Exception {
        when(service.send("airbnb", "Test article", "https://example.com/article"))
                .thenReturn(1);

        mockMvc.perform(post("/api/admin/slack/test-deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyKey": "airbnb",
                                  "title": "Test article",
                                  "articleUrl": "https://example.com/article"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyKey").value("airbnb"))
                .andExpect(jsonPath("$.sentChannelCount").value(1));

        verify(service).send("airbnb", "Test article", "https://example.com/article");
    }

    @Test
    void sendRejectsBlankRequestValues() throws Exception {
        mockMvc.perform(post("/api/admin/slack/test-deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyKey": "",
                                  "title": "",
                                  "articleUrl": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
