package com.globaltechblogarchive.slack.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CachedBodyHttpServletRequestTest {

    @Test
    void returnsCachedBodyForRepeatedReads() throws Exception {
        byte[] body = "command=%2Ftech&text=뉴스".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, body);

        assertThat(cachedRequest.getInputStream().readAllBytes()).isEqualTo(body);
        assertThat(cachedRequest.getInputStream().readAllBytes()).isEqualTo(body);
        assertThat(cachedRequest.getReader().readLine()).isEqualTo("command=%2Ftech&text=뉴스");
    }
}
