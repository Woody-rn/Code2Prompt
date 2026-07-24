package ru.npepub.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.filter.FileFilter;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Recursively scans a directory and reads text files as UTF-8 strings.
 */
@C2PComponent
class FileScannerImpl implements FileScanner {

    private static final Logger log = LoggerFactory.getLogger(FileScannerImpl.class);

    @C2PInject
    private FileFilter fileFilter;

    @Override
    public List<FileInfo> scan(Path rootDir) {
        validateDirectory(rootDir);
        log.info("Scanning directory: {}", rootDir);

        try (Stream<Path> stream = Files.walk(rootDir)) {
            List<FileInfo> files = stream
                    .filter(Files::isRegularFile)
                    .map(filePath -> toFileInfo(rootDir, filePath))
                    .flatMap(Optional::stream)
                    .toList();

            logScanResult(files);
            return files;
        } catch (IOException e) {
            log.error("Failed to scan directory: {}", rootDir, e);
            throw new RuntimeException("Directory scan failed: " + rootDir, e);
        }
    }

    private void validateDirectory(Path rootDir) {
        if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("Not a directory: " + rootDir);
        }
    }

    private Optional<FileInfo> toFileInfo(Path rootDir, Path filePath) {
        if (!fileFilter.shouldInclude(filePath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(filePath);
            FileInfo fileInfo = FileInfo.of(rootDir, filePath, content);
            log.debug("Scanned: {} ({} symbols)", fileInfo.relativePath(), fileInfo.size());
            return Optional.of(fileInfo);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", filePath, e);
            return Optional.empty();
        }
    }

    private void logScanResult(List<FileInfo> files) {
        int totalSymbols = files.stream().mapToInt(FileInfo::size).sum();
        log.info("Scan complete. {} files found, total {} symbols", files.size(), totalSymbols);
    }
}