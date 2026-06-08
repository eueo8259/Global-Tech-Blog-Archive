package com.globaltechblogarchive.crawl.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlMetadataExtractor {

    private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern DATE_PUBLISHED = Pattern.compile("(?is)datePublished[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']");

    private HtmlMetadataExtractor() {
    }

    public static String metaContent(String html, String name) {
        String escaped = Pattern.quote(name);
        Pattern nameThenContent = Pattern.compile(
                "(?is)<meta\\b(?=[^>]*(?:property|name|itemprop)=[\"']" + escaped + "[\"'])(?=[^>]*content=[\"']([^\"']*)[\"'])[^>]*>"
        );
        Matcher matcher = nameThenContent.matcher(html);
        if (matcher.find()) {
            return TextCleaner.clean(matcher.group(1));
        }
        return "";
    }

    public static String titleTag(String html) {
        Matcher matcher = TITLE.matcher(html);
        if (matcher.find()) {
            return TextCleaner.clean(matcher.group(1));
        }
        return "";
    }

    public static String datePublished(String html) {
        Matcher matcher = DATE_PUBLISHED.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
