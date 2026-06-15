package com.globaltechblogarchive.bootstrap.domain;

import java.util.List;

public record BootstrapArchive(
        BootstrapManifest manifest,
        List<BootstrapArticleRecord> articles,
        List<BootstrapAiDecisionRecord> aiDecisions
) {
}
