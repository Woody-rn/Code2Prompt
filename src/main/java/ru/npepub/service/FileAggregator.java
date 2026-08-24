package ru.npepub.service;

import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;

import java.util.List;

/**
 * Splits a list of files into chunks respecting the symbol limit.
 * When oneFilePerChunk is true, each file becomes a separate chunk.
 */
public interface FileAggregator {

    /**
     * @param files           files to split into chunks
     * @param symbolLimit     max symbols per chunk
     * @param oneFilePerChunk when true, each file goes into its own chunk
     * @return ordered list of chunks
     */
    List<Chunk> aggregate(List<FileInfo> files, int symbolLimit, boolean oneFilePerChunk);
}