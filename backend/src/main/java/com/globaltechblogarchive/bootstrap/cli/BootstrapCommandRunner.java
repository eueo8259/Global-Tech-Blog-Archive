package com.globaltechblogarchive.bootstrap.cli;

import com.globaltechblogarchive.bootstrap.application.BootstrapExportService;
import com.globaltechblogarchive.bootstrap.application.BootstrapImportService;
import com.globaltechblogarchive.bootstrap.application.BootstrapResult;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bootstrap.command")
public class BootstrapCommandRunner implements ApplicationRunner {

    private final BootstrapExportService exportService;
    private final BootstrapImportService importService;
    private final String command;
    private final Path file;

    public BootstrapCommandRunner(
            BootstrapExportService exportService,
            BootstrapImportService importService,
            @Value("${bootstrap.command}") String command,
            @Value("${bootstrap.file}") String file
    ) {
        this.exportService = exportService;
        this.importService = importService;
        this.command = command;
        this.file = Path.of(file);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if ("export".equals(command)) {
                printExportSuccess(exportService.exportTo(file));
                return;
            }
            if ("import".equals(command)) {
                printImportSuccess(importService.importFrom(file));
                return;
            }
            throw new IllegalArgumentException("Unsupported bootstrap command: " + command);
        } catch (RuntimeException exception) {
            System.err.println("BOOTSTRAP " + command.toUpperCase() + " FAILED");
            System.err.println("articles=0");
            System.err.println("aiDecisions=0");
            System.err.println("failed=1");
            System.err.println("reason=" + exception.getMessage());
            throw exception;
        }
    }

    private void printExportSuccess(BootstrapResult result) {
        System.out.println("BOOTSTRAP EXPORT SUCCESS");
        printCounts(result);
        System.out.println("sha256=" + result.sha256());
        System.out.println("file=" + result.file());
    }

    private void printImportSuccess(BootstrapResult result) {
        System.out.println("BOOTSTRAP IMPORT SUCCESS");
        printCounts(result);
    }

    private void printCounts(BootstrapResult result) {
        System.out.println("articles=" + result.articleCount());
        System.out.println("aiDecisions=" + result.aiDecisionCount());
        System.out.println("failed=0");
    }
}
