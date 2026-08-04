package ru.npepub.service.impl;

import ru.npepub.di.api.C2PComponent;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.ChunkFormatter;

/**
 * Formats chunks with separators and relative file paths.
 * <p>
 * Output format:
 * <pre>
 * ========================================
 * File: src/main/java/com/example/User.java
 * ========================================
 * [file content]
 * </pre>
 * Split files include part markers: [ЧАСТЬ 1/3], [ПРОДОЛЖЕНИЕ 2/3], [ОКОНЧАНИЕ 3/3].
 */
@C2PComponent
class ChunkFormatterImpl implements ChunkFormatter {

    private static final String SEPARATOR = "=".repeat(40);

    @Override
    public String format(Chunk chunk) {
        StringBuilder sb = new StringBuilder();

        for (FileInfo file : chunk.files()) {
            sb.append(SEPARATOR).append("\n");
            sb.append("File: ").append(file.relativePath());
            appendSplitMarker(file, sb);
            sb.append("\n").append(SEPARATOR).append("\n");
            sb.append(file.content()).append("\n");
        }

        return sb.toString();
    }

    private void appendSplitMarker(FileInfo file, StringBuilder sb) {
        if (file.isSplit()) {
            if (file.partIndex() == 1) {
                sb.append(" [ЧАСТЬ ")
                        .append(file.partIndex())
                        .append("/")
                        .append(file.totalParts())
                        .append("]");
            } else if (file.partIndex() == file.totalParts()) {
                sb.append(" [ОКОНЧАНИЕ ")
                        .append(file.partIndex())
                        .append("/")
                        .append(file.totalParts())
                        .append("]");
            } else {
                sb.append(" [ПРОДОЛЖЕНИЕ ")
                        .append(file.partIndex())
                        .append("/")
                        .append(file.totalParts())
                        .append("]");
            }
        }
    }
}