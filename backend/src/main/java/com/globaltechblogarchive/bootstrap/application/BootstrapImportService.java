package com.globaltechblogarchive.bootstrap.application;

import com.globaltechblogarchive.bootstrap.domain.BootstrapArchive;
import com.globaltechblogarchive.bootstrap.infrastructure.BootstrapArchiveFileStore;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BootstrapImportService {

    private final BootstrapArchiveFileStore fileStore;
    private final BootstrapImportWriter importWriter;

    public BootstrapResult importFrom(Path source) {
        BootstrapArchive archive = fileStore.read(source);
        BootstrapArchiveValidator.validate(archive);
        importWriter.importArchive(archive);
        return new BootstrapResult(
                archive.articles().size(),
                archive.aiDecisions().size(),
                null,
                source
        );
    }
}
