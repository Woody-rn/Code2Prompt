package ru.npepub.service;

import java.nio.file.Path;

/**
 * Resolves output file paths for chunks.
 */
public interface PathResolver {

    /**
     * @param outputDir directory for output files
     * @param chunkIndex chunk number (1-based)
     * @return full path to the output file
     */
    Path resolve(Path outputDir, int chunkIndex);
}