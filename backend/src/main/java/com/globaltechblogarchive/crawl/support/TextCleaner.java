package com.globaltechblogarchive.crawl.support;

import java.util.regex.Pattern;

public final class TextCleaner {

    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern ENTITY = Pattern.compile("&(#\\d+|#x[0-9a-fA-F]+|[a-zA-Z]+);");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TextCleaner() {
    }

    public static String clean(String value) {
        if (value == null) {
            return "";
        }
        String withoutTags = TAG.matcher(value).replaceAll(" ");
        String decoded = decodeBasicEntities(withoutTags);
        return WHITESPACE.matcher(decoded).replaceAll(" ").trim();
    }

    public static String shortContext(String value, String fallbackTitle) {
        String context = clean(value);
        if (context.isBlank()) {
            context = clean(fallbackTitle);
        }
        if (context.length() <= 500) {
            return context;
        }
        return context.substring(0, 500);
    }

    private static String decodeBasicEntities(String value) {
        String decoded = value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
        return ENTITY.matcher(decoded).replaceAll(" ");
    }
}
