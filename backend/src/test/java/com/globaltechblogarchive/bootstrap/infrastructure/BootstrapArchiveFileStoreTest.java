package com.globaltechblogarchive.bootstrap.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.bootstrap.domain.BootstrapAiDecisionRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArchive;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArticleRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapManifest;
import com.globaltechblogarchive.bootstrap.exception.BootstrapArchiveException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BootstrapArchiveFileStoreTest {

    @TempDir
    Path tempDir;

    private final BootstrapArchiveFileStore fileStore = new BootstrapArchiveFileStore(
            new ObjectMapper().registerModule(new JavaTimeModule())
    );

    @Test
    void writeAndReadPreservesArchiveAndCreatesChecksum() {
        Path target = tempDir.resolve("archive.ndjson");
        BootstrapArchive archive = archive();

        String checksum = fileStore.write(target, archive);
        BootstrapArchive restored = fileStore.read(target);

        assertThat(checksum).hasSize(64);
        assertThat(fileStore.checksumPath(target)).exists();
        assertThat(restored).isEqualTo(archive);
    }

    @Test
    void writeRejectsExistingTarget() throws Exception {
        Path target = tempDir.resolve("archive.ndjson");
        Files.writeString(target, "existing");

        assertThatThrownBy(() -> fileStore.write(target, archive()))
                .isInstanceOf(BootstrapArchiveException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void readRejectsChecksumMismatch() throws Exception {
        Path target = tempDir.resolve("archive.ndjson");
        fileStore.write(target, archive());
        Files.writeString(target, Files.readString(target) + "corrupted");

        assertThatThrownBy(() -> fileStore.read(target))
                .isInstanceOf(BootstrapArchiveException.class)
                .hasMessageContaining("checksum does not match");
    }

    private BootstrapArchive archive() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 15, 10, 30);
        BootstrapArticleRecord article = new BootstrapArticleRecord(
                BootstrapArticleRecord.RECORD_TYPE,
                "openai",
                "Translated title",
                "https://openai.com/news/article",
                "article-hash",
                ArticleCategory.AI,
                time,
                time,
                time
        );
        BootstrapAiDecisionRecord decision = new BootstrapAiDecisionRecord(
                BootstrapAiDecisionRecord.RECORD_TYPE,
                "openai",
                "article-hash",
                "https://openai.com/news/article",
                "Original title",
                "Translated title",
                ArticleCategory.AI,
                true,
                "gpt-5-mini",
                "v1",
                time,
                time
        );
        return new BootstrapArchive(
                new BootstrapManifest(BootstrapManifest.RECORD_TYPE, 1, time, 1, 1),
                List.of(article),
                List.of(decision)
        );
    }
}
