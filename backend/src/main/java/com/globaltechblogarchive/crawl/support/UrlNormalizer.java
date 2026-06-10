package com.globaltechblogarchive.crawl.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class UrlNormalizer {

    private UrlNormalizer() {
    }

    public static String absolute(String baseUrl, String url) {
        URI base = URI.create(baseUrl);
        return base.resolve(cleanUrl(url)).toString();
    }

    public static String normalize(String url) {
        try {
            URI uri = new URI(cleanUrl(url)).normalize();
            String scheme = lower(uri.getScheme());
            String host = lower(uri.getHost());
            int port = uri.getPort();
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }
            return new URI(scheme, uri.getUserInfo(), host, port, path, uri.getRawQuery(), null)
                    .toString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid URL: " + url, exception);
        }
    }

    private static String lower(String value) {
        if (value == null) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String cleanUrl(String url) {
        return url
                .replace("&amp;", "&")
                .replace("&#038;", "&")
                .replace("&#38;", "&")
                .trim();
    }

}
