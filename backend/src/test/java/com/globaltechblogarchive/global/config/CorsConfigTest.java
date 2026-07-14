package com.globaltechblogarchive.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorsConfigTest {

    private final Filter productionCorsFilter = corsFilter(
            "https://techport.dev",
            "https://www.techport.dev",
            "http://localhost:5173"
    );

    @Test
    void corsAllowsProductionFrontendOriginForApiRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/articles");
        request.addHeader("Origin", "https://techport.dev");
        MockHttpServletResponse response = new MockHttpServletResponse();

        productionCorsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("https://techport.dev");
    }

    @Test
    void corsRejectsUnknownOriginForApiRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/articles");
        request.addHeader("Origin", "https://example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        productionCorsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    void corsAllowsDevelopmentFrontendOnlyWhenConfigured() throws Exception {
        Filter developmentCorsFilter = corsFilter("https://techport-frontend-dev.vercel.app");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/articles");
        request.addHeader("Origin", "https://techport-frontend-dev.vercel.app");
        MockHttpServletResponse response = new MockHttpServletResponse();

        developmentCorsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("https://techport-frontend-dev.vercel.app");
    }

    @Test
    void corsHandlesApiPreflightRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/articles");
        request.addHeader("Origin", "https://www.techport.dev");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        productionCorsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("https://www.techport.dev");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .contains("GET");
    }

    @Test
    void corsDoesNotApplyOutsideApiPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        request.addHeader("Origin", "https://techport.dev");
        MockHttpServletResponse response = new MockHttpServletResponse();

        productionCorsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeaderNames()).doesNotContainAnyElementsOf(List.of(
                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS
        ));
    }

    private Filter corsFilter(String... allowedOrigins) {
        return new CorsConfig(new CorsProperties(List.of(allowedOrigins))).corsFilter();
    }
}
