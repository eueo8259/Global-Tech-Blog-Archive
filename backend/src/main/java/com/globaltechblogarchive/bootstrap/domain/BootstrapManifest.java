package com.globaltechblogarchive.bootstrap.domain;

import java.time.LocalDateTime;

public record BootstrapManifest(
        String recordType,
        int formatVersion,
        LocalDateTime exportedAt,
        long articleCount,
        long aiDecisionCount
) {

    public static final String RECORD_TYPE = "MANIFEST";
    public static final int CURRENT_FORMAT_VERSION = 1;

    public static BootstrapManifest create(long articleCount, long aiDecisionCount) {
        return new BootstrapManifest(
                RECORD_TYPE,
                CURRENT_FORMAT_VERSION,
                LocalDateTime.now(),
                articleCount,
                aiDecisionCount
        );
    }
}
