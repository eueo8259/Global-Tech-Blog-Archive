package com.globaltechblogarchive.crawl.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import com.globaltechblogarchive.crawl.exception.SourceFetchException;
import com.globaltechblogarchive.global.error.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SourceDocumentClientTest {

    private HttpServer server;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchReturnsResponseBodyForSuccessfulRequest() throws IOException {
        startServer(200, "<rss>articles</rss>");

        String body = new SourceDocumentClient(meterRegistry).fetch(url());

        assertThat(body).isEqualTo("<rss>articles</rss>");
        assertThat(meterRegistry.get("crawl.http.request.duration")
                .tag("host", "localhost")
                .tag("outcome", "success")
                .timer()
                .count())
                .isEqualTo(1);
    }

    @Test
    void fetchThrowsHttpStatusExceptionForNotFoundResponse() throws IOException {
        startServer(404, "not found");

        assertThatThrownBy(() -> new SourceDocumentClient(meterRegistry).fetch(url()))
                .isInstanceOfSatisfying(SourceFetchException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOURCE_FETCH_HTTP_STATUS_ERROR);
                })
                .hasMessage("Fetch failed with status 404");
        assertThat(meterRegistry.get("crawl.http.request.duration")
                .tag("host", "localhost")
                .tag("outcome", "http_status_error")
                .timer()
                .count())
                .isEqualTo(1);
    }

    @Test
    void fetchMapsSslHandshakeExceptionToTls() throws Exception {
        SSLHandshakeException cause = new SSLHandshakeException("certificate rejected");

        assertFailure(cause, ErrorCode.SOURCE_FETCH_TLS_ERROR);
    }

    @Test
    void fetchMapsHttpTimeoutExceptionToTimeout() throws Exception {
        HttpTimeoutException cause = new HttpTimeoutException("request timed out");

        assertFailure(cause, ErrorCode.SOURCE_FETCH_TIMEOUT_ERROR);
    }

    @Test
    void fetchMapsHttpConnectTimeoutExceptionToTimeout() throws Exception {
        HttpConnectTimeoutException cause = new HttpConnectTimeoutException("connect timed out");

        assertFailure(cause, ErrorCode.SOURCE_FETCH_TIMEOUT_ERROR);
    }

    @Test
    void fetchMapsUnknownHostExceptionToDns() throws Exception {
        UnknownHostException cause = new UnknownHostException("unknown.example");

        assertFailure(cause, ErrorCode.SOURCE_FETCH_DNS_ERROR);
    }

    @Test
    void fetchMapsConnectExceptionToConnection() throws Exception {
        ConnectException cause = new ConnectException("connection refused");

        assertFailure(cause, ErrorCode.SOURCE_FETCH_CONNECTION_ERROR);
    }

    @Test
    void fetchMapsOtherIOExceptionToNetwork() throws Exception {
        IOException cause = new IOException("connection reset");

        assertFailure(cause, ErrorCode.SOURCE_FETCH_NETWORK_ERROR);
    }

    @Test
    void fetchUsesCauseChainToClassifyFailure() throws Exception {
        IOException cause = new IOException("wrapped", new UnknownHostException("unknown.example"));

        assertFailure(cause, ErrorCode.SOURCE_FETCH_DNS_ERROR);
    }

    @Test
    void fetchMapsInterruptedExceptionAndRestoresInterruptFlag() throws Exception {
        HttpClient httpClient = throwingClient(new InterruptedException("interrupted"));

        try {
            assertThatThrownBy(() -> new SourceDocumentClient(httpClient, meterRegistry)
                    .fetch("https://example.com"))
                    .isInstanceOfSatisfying(SourceFetchException.class, exception -> {
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SOURCE_FETCH_INTERRUPTED_ERROR);
                    })
                    .hasMessage("Fetch interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private void startServer(int status, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(status, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
    }

    private String url() {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    private void assertFailure(IOException cause, ErrorCode expectedErrorCode) throws Exception {
        HttpClient httpClient = throwingClient(cause);

        assertThatThrownBy(() -> new SourceDocumentClient(httpClient, meterRegistry)
                .fetch("https://example.com"))
                .isInstanceOfSatisfying(SourceFetchException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
                });
    }

    @SuppressWarnings("unchecked")
    private HttpClient throwingClient(Exception exception) throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(exception);
        return httpClient;
    }
}
