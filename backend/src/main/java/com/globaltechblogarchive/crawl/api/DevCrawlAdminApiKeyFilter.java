package com.globaltechblogarchive.crawl.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Profile("dev")
@Component
public class DevCrawlAdminApiKeyFilter extends OncePerRequestFilter {

    static final String ADMIN_PATH_PREFIX = "/api/admin/article-crawls/";
    static final String API_KEY_HEADER = "X-Crawl-Admin-Key";

    private final String expectedKey;

    public DevCrawlAdminApiKeyFilter(
            @Value("${crawl.admin-api.key:}") String expectedKey
    ) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!matches(request.getHeader(API_KEY_HEADER))) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean matches(String suppliedKey) {
        if (expectedKey == null || expectedKey.isBlank() || suppliedKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
