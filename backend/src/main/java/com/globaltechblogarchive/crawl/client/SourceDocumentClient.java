package com.globaltechblogarchive.crawl.client;

import com.globaltechblogarchive.crawl.exception.SourceFetchException;
import com.globaltechblogarchive.global.error.ErrorCode;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLHandshakeException;
import org.springframework.stereotype.Component;

@Component
public class SourceDocumentClient {

    private final HttpClient httpClient;

    public SourceDocumentClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    SourceDocumentClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/125.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,"
                        + "application/rss+xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new SourceFetchException(
                        ErrorCode.SOURCE_FETCH_HTTP_STATUS_ERROR,
                        "Fetch failed with status " + response.statusCode()
                );
            }
            return response.body();
        } catch (IOException exception) {
            throw new SourceFetchException(
                    errorCode(exception),
                    "Fetch failed: " + exception.getMessage()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SourceFetchException(ErrorCode.SOURCE_FETCH_INTERRUPTED_ERROR, "Fetch interrupted");
        }
    }

    private ErrorCode errorCode(IOException exception) {
        if (hasCause(exception, SSLHandshakeException.class)) {
            return ErrorCode.SOURCE_FETCH_TLS_ERROR;
        }
        if (hasCause(exception, HttpTimeoutException.class)) {
            return ErrorCode.SOURCE_FETCH_TIMEOUT_ERROR;
        }
        if (hasCause(exception, UnknownHostException.class)) {
            return ErrorCode.SOURCE_FETCH_DNS_ERROR;
        }
        if (hasCause(exception, ConnectException.class)) {
            return ErrorCode.SOURCE_FETCH_CONNECTION_ERROR;
        }
        return ErrorCode.SOURCE_FETCH_NETWORK_ERROR;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current == current.getCause()) {
                return false;
            }
            current = current.getCause();
        }
        return false;
    }
}
