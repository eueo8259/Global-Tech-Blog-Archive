package com.globaltechblogarchive.bootstrap.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.bootstrap.domain.BootstrapAiDecisionRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArchive;
import com.globaltechblogarchive.bootstrap.domain.BootstrapArticleRecord;
import com.globaltechblogarchive.bootstrap.domain.BootstrapManifest;
import com.globaltechblogarchive.bootstrap.exception.BootstrapArchiveException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BootstrapArchiveFileStore {

    private static final String CHECKSUM_SUFFIX = ".sha256";

    private final ObjectMapper objectMapper;

    public BootstrapArchiveFileStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Path target, BootstrapArchive archive) {
        validateAbsolutePath(target);
        Path checksumPath = checksumPath(target);
        if (Files.exists(target) || Files.exists(checksumPath)) {
            throw new BootstrapArchiveException("Bootstrap output file already exists: " + target);
        }

        Path parent = target.getParent();
        Path temporaryFile = null;
        try {
            Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                writeLine(writer, archive.manifest());
                for (BootstrapArticleRecord article : archive.articles()) {
                    writeLine(writer, article);
                }
                for (BootstrapAiDecisionRecord decision : archive.aiDecisions()) {
                    writeLine(writer, decision);
                }
            }
            moveCompletedFile(temporaryFile, target);
            restrictToOwner(target);
            String checksum = sha256(target);
            Files.writeString(
                    checksumPath,
                    checksum + "  " + target.getFileName() + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
            restrictToOwner(checksumPath);
            return checksum;
        } catch (IOException exception) {
            throw new BootstrapArchiveException("Failed to write bootstrap archive: " + target, exception);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    public BootstrapArchive read(Path source) {
        validateAbsolutePath(source);
        verifyChecksum(source);

        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new BootstrapArchiveException("Bootstrap archive is empty: " + source);
            }
            BootstrapManifest manifest = parse(firstLine, BootstrapManifest.class);
            if (!BootstrapManifest.RECORD_TYPE.equals(manifest.recordType())) {
                throw new BootstrapArchiveException("First bootstrap record must be MANIFEST");
            }

            List<BootstrapArticleRecord> articles = new ArrayList<>();
            List<BootstrapAiDecisionRecord> decisions = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    throw new BootstrapArchiveException("Blank bootstrap record at line " + lineNumber);
                }
                JsonNode node = parseTree(line, lineNumber);
                String recordType = node.path("recordType").asText();
                if (BootstrapArticleRecord.RECORD_TYPE.equals(recordType)) {
                    articles.add(parse(line, BootstrapArticleRecord.class));
                } else if (BootstrapAiDecisionRecord.RECORD_TYPE.equals(recordType)) {
                    decisions.add(parse(line, BootstrapAiDecisionRecord.class));
                } else {
                    throw new BootstrapArchiveException(
                            "Unknown bootstrap record type at line " + lineNumber + ": " + recordType
                    );
                }
            }
            return new BootstrapArchive(manifest, List.copyOf(articles), List.copyOf(decisions));
        } catch (IOException exception) {
            throw new BootstrapArchiveException("Failed to read bootstrap archive: " + source, exception);
        }
    }

    public Path checksumPath(Path archivePath) {
        return archivePath.resolveSibling(archivePath.getFileName() + CHECKSUM_SUFFIX);
    }

    private void verifyChecksum(Path source) {
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new BootstrapArchiveException("Bootstrap archive is not readable: " + source);
        }
        Path checksumPath = checksumPath(source);
        if (!Files.isRegularFile(checksumPath) || !Files.isReadable(checksumPath)) {
            throw new BootstrapArchiveException("Bootstrap checksum file is not readable: " + checksumPath);
        }
        try {
            String checksumLine = Files.readString(checksumPath, StandardCharsets.UTF_8).trim();
            String[] parts = checksumLine.split("\\s+", 2);
            if (parts.length != 2 || !parts[1].equals(source.getFileName().toString())) {
                throw new BootstrapArchiveException("Invalid bootstrap checksum file: " + checksumPath);
            }
            String actual = sha256(source);
            if (!MessageDigest.isEqual(
                    parts[0].toLowerCase().getBytes(StandardCharsets.US_ASCII),
                    actual.getBytes(StandardCharsets.US_ASCII)
            )) {
                throw new BootstrapArchiveException("Bootstrap checksum does not match: " + source);
            }
        } catch (IOException exception) {
            throw new BootstrapArchiveException("Failed to read bootstrap checksum: " + checksumPath, exception);
        }
    }

    private void writeLine(BufferedWriter writer, Object value) throws IOException {
        writer.write(objectMapper.writeValueAsString(value));
        writer.newLine();
    }

    private <T> T parse(String line, Class<T> type) {
        try {
            return objectMapper.readValue(line, type);
        } catch (JsonProcessingException exception) {
            throw new BootstrapArchiveException("Invalid bootstrap JSON record", exception);
        }
    }

    private JsonNode parseTree(String line, int lineNumber) {
        try {
            return objectMapper.readTree(line);
        } catch (JsonProcessingException exception) {
            throw new BootstrapArchiveException("Invalid bootstrap JSON at line " + lineNumber, exception);
        }
    }

    private String sha256(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BootstrapArchiveException("Failed to calculate bootstrap checksum: " + path, exception);
        }
    }

    private void moveCompletedFile(Path temporaryFile, Path target) throws IOException {
        try {
            Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, target);
        }
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // Best-effort cleanup after a failed export.
        }
    }

    private void restrictToOwner(Path path) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        } catch (IOException | UnsupportedOperationException ignored) {
            // POSIX permissions are unavailable on Windows and some mounted filesystems.
        }
    }

    private void validateAbsolutePath(Path path) {
        if (!path.isAbsolute()) {
            throw new BootstrapArchiveException("Bootstrap file path must be absolute: " + path);
        }
        if (path.getParent() == null) {
            throw new BootstrapArchiveException("Bootstrap file path must have a parent directory: " + path);
        }
    }
}
