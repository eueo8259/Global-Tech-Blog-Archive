package com.globaltechblogarchive.bootstrap.application;

import java.nio.file.Path;

public record BootstrapResult(
        long articleCount,
        long aiDecisionCount,
        String sha256,
        Path file
) {
}
