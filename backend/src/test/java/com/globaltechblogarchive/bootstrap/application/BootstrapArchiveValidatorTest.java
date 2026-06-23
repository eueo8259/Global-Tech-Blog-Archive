package com.globaltechblogarchive.bootstrap.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.bootstrap.domain.BootstrapArchive;
import com.globaltechblogarchive.bootstrap.domain.BootstrapManifest;
import com.globaltechblogarchive.bootstrap.exception.BootstrapArchiveException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BootstrapArchiveValidatorTest {

    @Test
    void validateRejectsUnsupportedFormatVersion() {
        BootstrapArchive archive = new BootstrapArchive(
                new BootstrapManifest("MANIFEST", 2, LocalDateTime.now(), 0, 0),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> BootstrapArchiveValidator.validate(archive))
                .isInstanceOf(BootstrapArchiveException.class)
                .hasMessageContaining("Unsupported bootstrap format version");
    }

    @Test
    void validateRejectsManifestCountMismatch() {
        BootstrapArchive archive = new BootstrapArchive(
                new BootstrapManifest("MANIFEST", 1, LocalDateTime.now(), 1, 0),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> BootstrapArchiveValidator.validate(archive))
                .isInstanceOf(BootstrapArchiveException.class)
                .hasMessageContaining("article count");
    }
}
