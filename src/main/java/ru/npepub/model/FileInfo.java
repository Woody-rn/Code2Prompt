package ru.npepub.model;

import java.nio.file.Path;

/**
 * Represents a scanned file with its relative path and content.
 */
public record FileInfo(
        Path relativePath,
        String content,
        int size
) {
    /**
     * Creates a FileInfo from an absolute file path and its content.
     *
     * @param rootDir  the root directory of scanning
     * @param filePath absolute path to the file
     * @param content  file content as string
     * @return new FileInfo with relative path computed from rootDir
     */
    public static FileInfo of(Path rootDir, Path filePath, String content) {
        return new FileInfo(
                rootDir.relativize(filePath),
                content,
                content.length()
        );
    }
}