package ru.npepub.service;

import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;

import java.util.List;

/**
 * Splits a list of files into chunks respecting the symbol limit.
 * Files are never split — a file goes entirely into one chunk or the next.
 */
public interface FileAggregator {

    /**
     * @param files      files to split into chunks
     * @param symbolLimit max symbols per chunk
     * @return ordered list of chunks
     */
    List<Chunk> aggregate(List<FileInfo> files, int symbolLimit);
}