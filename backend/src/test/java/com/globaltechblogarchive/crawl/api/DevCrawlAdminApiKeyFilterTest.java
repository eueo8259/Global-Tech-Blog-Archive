package com.globaltechblogarchive.crawl.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DevCrawlAdminApiKeyFilterTest {

    private static final String API_KEY = "test-admin-key";

    private final DevCrawlAdminApiKeyFilter filter = new DevCrawlAdminApiKeyFilter(API_KEY);

    @Test
    void validKeyAllowsCrawlAdminRequest() throws Exception {
        MockHttpServletRequest request = request("/api/admin/article-crawls/backfill-run");
        request.addHeader(DevCrawlAdminApiKeyFilter.API_KEY_HEADER, API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void missingKeyRejectsCrawlAdminRequest() throws Exception {
        MockHttpServletRequest request = request("/api/admin/article-crawls/backfill-run");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void missingServerKeyRejectsCrawlAdminRequest() throws Exception {
        DevCrawlAdminApiKeyFilter disabledFilter = new DevCrawlAdminApiKeyFilter("");
        MockHttpServletRequest request = request("/api/admin/article-crawls/backfill-run");
        request.addHeader(DevCrawlAdminApiKeyFilter.API_KEY_HEADER, API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        disabledFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void unrelatedRequestBypassesFilter() throws Exception {
        MockHttpServletRequest request = request("/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        return request;
    }
}
