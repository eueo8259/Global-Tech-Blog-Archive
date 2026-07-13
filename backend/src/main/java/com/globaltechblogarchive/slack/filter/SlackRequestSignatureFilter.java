package com.globaltechblogarchive.slack.filter;

import com.globaltechblogarchive.slack.support.CachedBodyHttpServletRequest;
import com.globaltechblogarchive.slack.support.SlackRequestSignatureVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class SlackRequestSignatureFilter extends OncePerRequestFilter {

    private static final String SLACK_COMMAND_PATH = "/slack/commands";
    private static final String SIGNATURE_HEADER = "X-Slack-Signature";
    private static final String TIMESTAMP_HEADER = "X-Slack-Request-Timestamp";

    private final SlackRequestSignatureVerifier signatureVerifier;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !SLACK_COMMAND_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        byte[] rawBody = request.getInputStream().readAllBytes();
        if (!signatureVerifier.verify(
                request.getHeader(TIMESTAMP_HEADER),
                request.getHeader(SIGNATURE_HEADER),
                rawBody
        )) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        filterChain.doFilter(new CachedBodyHttpServletRequest(request, rawBody), response);
    }
}
