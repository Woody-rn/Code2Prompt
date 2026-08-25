package ru.npepub.service;

import ru.npepub.model.Chunk;

/**
 * Formats a chunk into a string suitable for output.
 */
public interface ChunkFormatter {

    /**
     * @param chunk     chunk to format
     * @param separator separator to use between files
     * @return formatted string with file headers and contents
     */
    String format(Chunk chunk, String separator);
}