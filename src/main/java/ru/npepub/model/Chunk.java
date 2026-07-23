package ru.npepub.model;

import java.util.Collections;
import java.util.List;

/**
 * A group of files that fits within the symbol limit.
 * Atomic unit for writing to a single output txt file.
 */

public record Chunk(
        int index,
        List<FileInfo> files,
        int totalSize
) {
    public Chunk {
        files = Collections.unmodifiableList(files);
    }

    /**
     * @param limit the maximum allowed size in symbols
     * @return how many symbols can still be added
     */
    public int remainingSpace(int limit) {
        return limit - totalSize;
    }

    /**
     * @param fileInfo the file to check
     * @param limit    the maximum allowed size in symbols
     * @return true if the file fits into this chunk
     */
    public boolean canFit(FileInfo fileInfo, int limit) {
        return totalSize + fileInfo.size() <= limit;
    }
}
