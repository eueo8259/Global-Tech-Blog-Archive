package com.globaltechblogarchive.slack.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.filter.SlackRequestSignatureFilter;
import com.globaltechblogarchive.slack.support.SlackRequestSignatureVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SlackRequestSignatureFilterTest {

    private static final String TIMESTAMP = "1531420618";
    private static final String SIGNATURE = "v0=signature";
    private static final byte[] RAW_BODY = "command=%2Ftech&text=java".getBytes(StandardCharsets.UTF_8);

    private final SlackRequestSignatureVerifier verifier = mock(SlackRequestSignatureVerifier.class);
    private final SlackRequestSignatureFilter filter = new SlackRequestSignatureFilter(verifier);

    @Test
    void validSignaturePassesRequestWithReadableRawBody() throws Exception {
        MockHttpServletRequest request = slackCommandRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<byte[]> downstreamBody = new AtomicReference<>();
        FilterChain chain = (ServletRequest downstreamRequest, ServletResponse downstreamResponse) ->
                downstreamBody.set(downstreamRequest.getInputStream().readAllBytes());
        when(verifier.verify(TIMESTAMP, SIGNATURE, RAW_BODY)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertThat(downstreamBody.get()).isEqualTo(RAW_BODY);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void interactivityRequestRequiresSignatureVerification() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                HttpMethod.POST.name(),
                "/api/slack/interactivity"
        );
        request.setServletPath("/api/slack/interactivity");
        request.addHeader("X-Slack-Request-Timestamp", TIMESTAMP);
        request.addHeader("X-Slack-Signature", SIGNATURE);
        request.setContent(RAW_BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(verifier.verify(TIMESTAMP, SIGNATURE, RAW_BODY)).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(verifier).verify(TIMESTAMP, SIGNATURE, RAW_BODY);
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(response));
    }

    @Test
    void invalidSignatureReturnsUnauthorizedWithoutCallingChain() throws Exception {
        MockHttpServletRequest request = slackCommandRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(verifier.verify(TIMESTAMP, SIGNATURE, RAW_BODY)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void oauthCallbackBypassesSignatureVerification() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/api/slack/oauth/callback");
        request.setServletPath("/api/slack/oauth/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(verifier, never()).verify(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(chain).doFilter(request, response);
    }

    @Test
    void getRequestToCommandPathBypassesSignatureVerification() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/api/slack/commands");
        request.setServletPath("/api/slack/commands");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(verifier, never()).verify(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest slackCommandRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST.name(), "/api/slack/commands");
        request.setServletPath("/api/slack/commands");
        request.addHeader("X-Slack-Request-Timestamp", TIMESTAMP);
        request.addHeader("X-Slack-Signature", SIGNATURE);
        request.setContent(RAW_BODY);
        return request;
    }
}
