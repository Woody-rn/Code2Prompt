package ru.npepub.service;

import ru.npepub.model.Chunk;

import java.nio.file.Path;
import java.util.List;

/**
 * Writes chunks to txt files on disk.
 */
public interface OutputWriter {

    /**
     * @param chunks      chunks to write
     * @param outputDir   directory for output files
     * @return list of created file paths
     */
    List<Path> write(List<Chunk> chunks, Path outputDir);
}