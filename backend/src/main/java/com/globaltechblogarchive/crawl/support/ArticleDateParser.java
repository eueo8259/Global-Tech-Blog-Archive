package com.globaltechblogarchive.crawl.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public final class ArticleDateParser {

    private static final List<DateTimeFormatter> OFFSET_DATE_FORMATTERS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME
    );

    private static final List<DateTimeFormatter> LIST_PAGE_DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    );

    private ArticleDateParser() {
    }

    public static LocalDateTime parseFeedDate(String value) {
        LocalDateTime offsetDate = parseOffsetDate(value);
        if (offsetDate != null) {
            return offsetDate;
        }
        return parseLocalDateTime(value);
    }

    public static LocalDateTime parseSitemapDate(String value) {
        LocalDateTime feedDate = parseFeedDate(value);
        if (feedDate != null) {
            return feedDate;
        }
        return parseLocalDate(value, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDateTime parseListPageDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : LIST_PAGE_DATE_FORMATTERS) {
            LocalDateTime parsed = parseLocalDate(value, formatter);
            if (parsed != null) {
                return parsed;
            }
        }
        return parseLocalDateTime(value);
    }

    public static LocalDateTime parseWordPressDate(String value) {
        return parseLocalDateTime(value);
    }

    private static LocalDateTime parseOffsetDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : OFFSET_DATE_FORMATTERS) {
            try {
                return ZonedDateTime.parse(value, formatter).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                try {
                    return OffsetDateTime.parse(value, formatter).toLocalDateTime();
                } catch (DateTimeParseException ignoredAgain) {
                    // Try next supported offset date shape.
                }
            }
        }
        return null;
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static LocalDateTime parseLocalDate(String value, DateTimeFormatter formatter) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, formatter).atStartOfDay();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
