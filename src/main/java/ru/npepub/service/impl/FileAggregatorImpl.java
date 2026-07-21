package ru.npepub.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.C2PComponent;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits files into chunks by symbol limit.
 * Files are atomic — never split across chunks.
 */

@C2PComponent
class FileAggregatorImpl implements FileAggregator {

    private static final Logger log = LoggerFactory.getLogger(FileAggregatorImpl.class);

    @Override
    public List<Chunk> aggregate(List<FileInfo> files, int symbolLimit) {
        log.info("Aggregating {} files with limit {} symbols", files.size(), symbolLimit);

        List<Chunk> chunks = new ArrayList<>();
        List<FileInfo> currentFiles = new ArrayList<>();
        int currentSize = 0;
        int chunkIndex = 1;

        for (FileInfo file : files) {
            int headerSize = estimateHeaderSize(file);
            int totalFileSize = file.size() + headerSize;

            if (totalFileSize > symbolLimit) {
                log.warn("File '{}' ({} symbols) exceeds the symbol limit ({}). " +
                                "It will be placed in a separate chunk.",
                        file.relativePath(), totalFileSize, symbolLimit);
            }

            if (currentSize + totalFileSize > symbolLimit && !currentFiles.isEmpty()) {
                chunks.add(new Chunk(chunkIndex++, List.copyOf(currentFiles), currentSize));
                currentFiles.clear();
                currentSize = 0;
            }

            currentFiles.add(file);
            currentSize += totalFileSize;
        }

        if (!currentFiles.isEmpty()) {
            chunks.add(new Chunk(chunkIndex, List.copyOf(currentFiles), currentSize));
        }

        log.info("Aggregation complete. {} chunks created", chunks.size());
        return chunks;
    }

    private int estimateHeaderSize(FileInfo file) {
        // "========================================\n" +
        // "File: relative/path/Name.java\n" +
        // "========================================\n"
        return 80 + file.relativePath().toString().length();
    }
}