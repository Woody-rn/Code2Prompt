package ru.npepub.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.config.FilterConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.exception.FilterMisuseException;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.DirectoryFilter;
import ru.npepub.filter.FileFilter;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileScanner;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@C2PComponent
class FileScannerImpl implements FileScanner {

    private static final Logger log = LoggerFactory.getLogger(FileScannerImpl.class);

    @C2PInject
    private FileFilter fileFilter;
    @C2PInject
    private DirectoryFilter directoryFilter;
    @C2PInject
    private ConfigPort configPort;

    @Override
    public List<FileInfo> scan(Path rootDir) {
        validateDirectory(rootDir);
        log.info("Scanning directory: {}", rootDir);

        FilterConfig filterConfig = configPort.load().filter();
        reloadConfig(filterConfig);

        List<FileInfo> files = new ArrayList<>();

        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(rootDir)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!directoryFilter.shouldEnter(dir)) {
                        log.debug("Skipping directory: {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) {
                        toFileInfo(rootDir, file).ifPresent(files::add);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Failed to visit: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failed to scan directory: {}", rootDir, e);
            throw new RuntimeException("Directory scan failed: " + rootDir, e);
        }

        logScanResult(files);
        return files;
    }

    private void reloadConfig(FilterConfig config) {
        if (fileFilter instanceof ConfigurableFilter c) {
            c.reloadConfig(config);
        }
        if (directoryFilter instanceof ConfigurableFilter c) {
            c.reloadConfig(config);
        }
    }

    private void validateDirectory(Path rootDir) {
        if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("Not a directory: " + rootDir);
        }
    }

    private Optional<FileInfo> toFileInfo(Path rootDir, Path filePath) {
        try {
            if (!fileFilter.shouldInclude(filePath)) {
                return Optional.empty();
            }
        } catch (FilterMisuseException e) {
            // A filter was called with a path it can't handle (e.g. root).
            // This is a bug in the filter, not a data error — log it
            // and skip the file so one bad entry doesn't abort the scan.
            log.warn("Filter misuse for {}: {}", filePath, e.getMessage());
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