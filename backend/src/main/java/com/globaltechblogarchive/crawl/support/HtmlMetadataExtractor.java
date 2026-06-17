package com.globaltechblogarchive.crawl.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlMetadataExtractor {

    private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern DATE_PUBLISHED = Pattern.compile("(?is)datePublished[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']");
    private static final Pattern SCRIPT_STYLE_SVG = Pattern.compile("(?is)<(script|style|svg)\\b[^>]*>.*?</\\1>");
    private static final Pattern PARAGRAPH = Pattern.compile("(?is)<p\\b[^>]*>(.*?)</p>");
    private static final String DATE_TEXT = "(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},\\s+\\d{4}";

    private HtmlMetadataExtractor() {
    }

    public static String metaContent(String html, String name) {
        String escaped = Pattern.quote(name);
        Pattern nameThenContent = Pattern.compile(
                "(?is)<meta\\b(?=[^>]*(?:property|name|itemprop)=[\"']" + escaped
                        + "[\"'])(?=[^>]*content=([\"'])(.*?)\\1)[^>]*>"
        );
        Matcher matcher = nameThenContent.matcher(html);
        if (matcher.find()) {
            return TextCleaner.clean(matcher.group(2));
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

    public static String heading(String html, int level) {
        Pattern pattern = Pattern.compile("(?is)<h" + level + "\\b[^>]*>(.*?)</h" + level + ">");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return TextCleaner.clean(matcher.group(1));
        }
        return "";
    }

    public static String firstDateAfterHeading(String html, int level) {
        Pattern pattern = Pattern.compile(
                "(?is)<h" + level + "\\b[^>]*>.*?</h" + level + ">.{0,5000}?(" + DATE_TEXT + ")"
        );
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return TextCleaner.clean(matcher.group(1));
        }
        return "";
    }

    public static String jsonLdText(String html, String fieldName) {
        String escaped = Pattern.quote(fieldName);
        Pattern pattern = Pattern.compile(
                "(?is)<script\\b[^>]*type=[\"']application/ld\\+json[\"'][^>]*>.*?[\"']"
                        + escaped + "[\"']\\s*:\\s*[\"']([^\"']*)[\"'].*?</script>"
        );
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return TextCleaner.clean(matcher.group(1));
        }
        return "";
    }

    public static String firstParagraph(String html) {
        Matcher matcher = PARAGRAPH.matcher(removeNonContentElements(html));
        while (matcher.find()) {
            String paragraph = TextCleaner.clean(matcher.group(1));
            if (!paragraph.isBlank()) {
                return paragraph;
            }
        }
        return "";
    }

    private static String removeNonContentElements(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return SCRIPT_STYLE_SVG.matcher(html).replaceAll(" ");
    }
}
