package ru.npepub.filter;

import java.nio.file.Path;

/**
 * Validates whether a file should be included in scanning.
 */
public interface FileFilter {

    /**
     * @param filePath path to the file
     * @return true if the file should be included
     */
    boolean shouldInclude(Path filePath);
}