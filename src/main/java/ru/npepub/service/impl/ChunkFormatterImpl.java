package ru.npepub.service.impl;

import ru.npepub.di.api.C2PComponent;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.ChunkFormatter;

/**
 * Formats chunks with separators and relative file paths.
 */

@C2PComponent
class ChunkFormatterImpl implements ChunkFormatter {

    private static final String SEPARATOR = "=".repeat(40);

    @Override
    public String format(Chunk chunk) {
        StringBuilder sb = new StringBuilder();

        for (FileInfo file : chunk.files()) {
            sb.append(SEPARATOR).append("\n");
            sb.append("File: ").append(file.relativePath()).append("\n");
            sb.append(SEPARATOR).append("\n");
            sb.append(file.content()).append("\n");
        }

        return sb.toString();
    }
}