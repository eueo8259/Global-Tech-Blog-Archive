package com.globaltechblogarchive.bootstrap.application;

import com.globaltechblogarchive.bootstrap.domain.BootstrapAiDecisionRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArchive;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArticleRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapManifest;
import com.globaltechblogarchive.bootstrap.exception.BootstrapArchiveException;

final class BootstrapArchiveValidator {

    private BootstrapArchiveValidator() {
    }

    static void validate(BootstrapArchive archive) {
        BootstrapManifest manifest = archive.manifest();
        if (manifest.formatVersion() != BootstrapManifest.CURRENT_FORMAT_VERSION) {
            throw new BootstrapArchiveException(
                    "Unsupported bootstrap format version: " + manifest.formatVersion()
            );
        }
        requireNonNull(manifest.exportedAt(), "manifest.exportedAt");
        if (manifest.articleCount() != archive.articles().size()) {
            throw new BootstrapArchiveException("Bootstrap article count does not match manifest");
        }
        if (manifest.aiDecisionCount() != archive.aiDecisions().size()) {
            throw new BootstrapArchiveException("Bootstrap AI decision count does not match manifest");
        }
        archive.articles().forEach(BootstrapArchiveValidator::validateArticle);
        archive.aiDecisions().forEach(BootstrapArchiveValidator::validateDecision);
    }

    private static void validateArticle(BootstrapArticleRecord article) {
        requireRecordType(article.recordType(), BootstrapArticleRecord.RECORD_TYPE);
        requireText(article.companyKey(), "article.companyKey");
        requireText(article.title(), "article.title");
        requireText(article.articleUrl(), "article.articleUrl");
        requireText(article.articleUrlHash(), "article.articleUrlHash");
        requireNonNull(article.category(), "article.category");
        requireNonNull(article.publishedAt(), "article.publishedAt");
        requireNonNull(article.createdAt(), "article.createdAt");
        requireNonNull(article.updatedAt(), "article.updatedAt");
    }

    private static void validateDecision(BootstrapAiDecisionRecord decision) {
        requireRecordType(decision.recordType(), BootstrapAiDecisionRecord.RECORD_TYPE);
        requireText(decision.companyKey(), "aiDecision.companyKey");
        requireText(decision.articleUrlHash(), "aiDecision.articleUrlHash");
        requireText(decision.articleUrl(), "aiDecision.articleUrl");
        requireText(decision.originalTitle(), "aiDecision.originalTitle");
        requireText(decision.translatedTitle(), "aiDecision.translatedTitle");
        requireNonNull(decision.category(), "aiDecision.category");
        requireText(decision.model(), "aiDecision.model");
        requireText(decision.promptVersion(), "aiDecision.promptVersion");
        requireNonNull(decision.createdAt(), "aiDecision.createdAt");
        requireNonNull(decision.updatedAt(), "aiDecision.updatedAt");
    }

    private static void requireRecordType(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new BootstrapArchiveException("Invalid bootstrap record type: " + actual);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BootstrapArchiveException("Missing bootstrap field: " + field);
        }
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new BootstrapArchiveException("Missing bootstrap field: " + field);
        }
    }
}
