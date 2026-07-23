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
}
