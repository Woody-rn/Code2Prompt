package ru.npepub.model;

import java.nio.file.Path;

/**
 * Represents a scanned file with its relative path and content.
 * Supports splitting large files into multiple parts.
 */
public record FileInfo(
        Path relativePath,
        String content,
        int size,
        boolean isSplit,
        int partIndex,
        int totalParts
) {
    /**
     * Creates a FileInfo from an absolute file path and its content.
     */
    public static FileInfo of(Path rootDir, Path filePath, String content) {
        return new FileInfo(
                rootDir.relativize(filePath),
                content,
                content.length(),
                false, 0, 0
        );
    }

    /**
     * Creates a split part of a file.
     */
    public static FileInfo split(FileInfo original, String partContent, int partIndex, int totalParts) {
        return new FileInfo(
                original.relativePath(),
                partContent,
                partContent.length(),
                true, partIndex, totalParts
        );
    }
}