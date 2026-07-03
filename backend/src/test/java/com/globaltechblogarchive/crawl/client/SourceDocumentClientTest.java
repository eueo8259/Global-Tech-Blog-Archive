package com.globaltechblogarchive.crawl.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SourceDocumentClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchReturnsResponseBodyForSuccessfulRequest() throws IOException {
        startServer(200, "<rss>articles</rss>");

        String body = new SourceDocumentClient().fetch(url());

        assertThat(body).isEqualTo("<rss>articles</rss>");
    }

    @Test
    void fetchThrowsExceptionForClientErrorResponse() throws IOException {
        startServer(400, "bad request");

        assertThatThrownBy(() -> new SourceDocumentClient().fetch(url()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fetch failed with status 400");
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
}
