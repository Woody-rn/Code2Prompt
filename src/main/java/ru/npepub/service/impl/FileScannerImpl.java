package ru.npepub.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.C2PComponent;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Recursively scans a directory and reads all files as UTF-8 strings.
 */

@C2PComponent
class FileScannerImpl implements FileScanner {

    private static final Logger log = LoggerFactory.getLogger(FileScannerImpl.class);

    @Override
    public List<FileInfo> scan(Path rootDir) {
        validateDirectory(rootDir);
        log.info("Scanning directory: {}", rootDir);

        List<FileInfo> files = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(filePath -> addFileInfo(rootDir, filePath, files));
        } catch (IOException e) {
            log.error("Failed to scan directory: {}", rootDir, e);
            throw new RuntimeException("Directory scan failed: " + rootDir, e);
        }

        logScanResult(files);
        return files;
    }

    private void validateDirectory(Path rootDir) {
        if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("Not a directory: " + rootDir);
        }
    }

    private void addFileInfo(Path rootDir, Path filePath, List<FileInfo> files) {
        try {
            String content = Files.readString(filePath);
            FileInfo fileInfo = FileInfo.of(rootDir, filePath, content);
            files.add(fileInfo);
            log.debug("Scanned: {} ({} symbols)", fileInfo.relativePath(), fileInfo.size());
        } catch (IOException e) {
            log.warn("Failed to read file: {}", filePath, e);
        }
    }

    private void logScanResult(List<FileInfo> files) {
        int totalSymbols = files.stream().mapToInt(FileInfo::size).sum();
        log.info("Scan complete. {} files found, total {} symbols", files.size(), totalSymbols);
    }
}