package com.globaltechblogarchive.crawl.client;

import com.globaltechblogarchive.crawl.exception.SourceFetchException;
import com.globaltechblogarchive.global.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLHandshakeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SourceDocumentClient {

    private final HttpClient httpClient;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeHttpRequests = new AtomicInteger();

    @Autowired
    public SourceDocumentClient(MeterRegistry meterRegistry) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), meterRegistry);
    }

    SourceDocumentClient(HttpClient httpClient, MeterRegistry meterRegistry) {
        this.httpClient = httpClient;
        this.meterRegistry = meterRegistry;
    }

    public String fetch(String url) {
        return fetch("unknown", url);
    }

    public String fetch(String sourceKey, String url) {
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/125.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,"
                        + "application/rss+xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();

        Timer.Sample sample = Timer.start(meterRegistry);
        long startedAt = System.nanoTime();
        int activeRequests = activeHttpRequests.incrementAndGet();
        AtomicLong headersReceivedAt = new AtomicLong();
        String outcome = "unknown";
        int statusCode = 0;
        int responseChars = 0;
        long contentLength = -1;
        String protocol = "unknown";
        int redirectCount = 0;
        try {
            HttpResponse.BodyHandler<String> bodyHandler = responseInfo -> {
                headersReceivedAt.compareAndSet(0, System.nanoTime());
                return HttpResponse.BodyHandlers.ofString().apply(responseInfo);
            };
            HttpResponse<String> response = httpClient.send(request, bodyHandler);
            statusCode = response.statusCode();
            responseChars = response.body().length();
            contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            protocol = response.version().name();
            redirectCount = redirectCount(response);
            if (response.statusCode() >= 400) {
                outcome = "http_status_error";
                throw new SourceFetchException(
                        ErrorCode.SOURCE_FETCH_HTTP_STATUS_ERROR,
                        "Fetch failed with status " + response.statusCode()
                );
            }
            outcome = "success";
            return response.body();
        } catch (IOException exception) {
            ErrorCode errorCode = errorCode(exception);
            outcome = errorCode.name().toLowerCase(Locale.ROOT);
            throw new SourceFetchException(
                    errorCode,
                    "Fetch failed: " + exception.getMessage()
            );
        } catch (InterruptedException exception) {
            outcome = "interrupted";
            Thread.currentThread().interrupt();
            throw new SourceFetchException(ErrorCode.SOURCE_FETCH_INTERRUPTED_ERROR, "Fetch interrupted");
        } finally {
            long finishedAt = System.nanoTime();
            long headersAt = headersReceivedAt.get();
            long totalNanos = finishedAt - startedAt;
            long headersNanos = durationUntilHeaders(startedAt, headersAt);
            long bodyNanos = bodyDuration(headersAt, finishedAt);
            sample.stop(Timer.builder("crawl.http.request.duration")
                    .tag("host", host(uri))
                    .tag("outcome", outcome)
                    .register(meterRegistry));
            recordDuration("crawl.http.response.headers.duration", uri, outcome, headersNanos);
            recordDuration("crawl.http.response.body.duration", uri, outcome, bodyNanos);
            int activeRequestsAfter = activeHttpRequests.decrementAndGet();
            log.info(
                    "crawl_http_request_measurement sourceKey={} host={} path={} durationMs={} "
                            + "headersMs={} bodyMs={} outcome={} statusCode={} responseChars={} "
                            + "contentLength={} protocol={} redirectCount={} activeRequests={} "
                            + "activeRequestsAfter={} thread={}",
                    sourceKey,
                    host(uri),
                    path(uri),
                    TimeUnit.NANOSECONDS.toMillis(totalNanos),
                    millis(headersNanos),
                    millis(bodyNanos),
                    outcome,
                    statusCode,
                    responseChars,
                    contentLength,
                    protocol,
                    redirectCount,
                    activeRequests,
                    activeRequestsAfter,
                    Thread.currentThread().getName()
            );
        }
    }

    private String host(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return "unknown";
        }
        return host;
    }

    private String path(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path;
    }

    private long durationUntilHeaders(long startedAt, long headersAt) {
        if (headersAt == 0) {
            return -1;
        }
        return headersAt - startedAt;
    }

    private long bodyDuration(long headersAt, long finishedAt) {
        if (headersAt == 0) {
            return -1;
        }
        return finishedAt - headersAt;
    }

    private long millis(long durationNanos) {
        if (durationNanos < 0) {
            return -1;
        }
        return TimeUnit.NANOSECONDS.toMillis(durationNanos);
    }

    private void recordDuration(String metricName, URI uri, String outcome, long durationNanos) {
        if (durationNanos < 0) {
            return;
        }
        Timer.builder(metricName)
                .tag("host", host(uri))
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private int redirectCount(HttpResponse<?> response) {
        int count = 0;
        HttpResponse<?> current = response;
        while (current.previousResponse().isPresent()) {
            count++;
            current = current.previousResponse().orElseThrow();
        }
        return count;
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
