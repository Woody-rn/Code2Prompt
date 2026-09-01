package ru.npepub.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits files into chunks by symbol limit.
 * Supports per-file mode where each file becomes a single chunk.
 */
@C2PComponent
class FileAggregatorImpl implements FileAggregator {

    private static final Logger log = LoggerFactory.getLogger(FileAggregatorImpl.class);

    private static final int HEADER_BASE_SIZE = 80;
    private static final int CHUNK_FILL_THRESHOLD = 95;
    private static final int SPLIT_SAFETY_MARGIN = 100;

    @Override
    public List<Chunk> aggregate(List<FileInfo> files, int symbolLimit, boolean oneFilePerChunk) {
        log.info("Aggregating {} files with limit {} symbols, oneFilePerChunk={}",
                files.size(), symbolLimit, oneFilePerChunk);

        if (files.isEmpty()) {
            log.info("No files to aggregate");
            return List.of();
        }

        List<Chunk> chunks = oneFilePerChunk
                ? aggregatePerFile(files, symbolLimit)
                : aggregateByLimit(files, symbolLimit);

        logAggregationResult(chunks);
        return chunks;
    }

    /**
     * Each file goes into its own chunk. Large files are split.
     */
    private List<Chunk> aggregatePerFile(List<FileInfo> files, int symbolLimit) {
        List<Chunk> chunks = new ArrayList<>();
        int chunkIndex = 1;

        for (FileInfo file : files) {
            List<FileInfo> parts = splitIfNeeded(file, symbolLimit);
            for (FileInfo part : parts) {
                chunks.add(createChunk(chunkIndex++, List.of(part)));
            }
        }

        return chunks;
    }

    /**
     * Files are packed into chunks until the limit is reached.
     * Files exceeding the limit are split across multiple chunks.
     */
    private List<Chunk> aggregateByLimit(List<FileInfo> files, int symbolLimit) {
        List<Chunk> chunks = new ArrayList<>();
        List<FileInfo> currentFiles = new ArrayList<>();
        int currentSize = 0;
        int chunkIndex = 1;

        for (FileInfo file : files) {
            List<FileInfo> parts = splitIfNeeded(file, symbolLimit);

            for (FileInfo part : parts) {
                int partSize = calculateTotalSize(part);

                if (!currentFiles.isEmpty() && currentSize + partSize > symbolLimit) {
                    chunks.add(createChunk(chunkIndex++, List.copyOf(currentFiles)));
                    currentFiles = new ArrayList<>();
                    currentSize = 0;
                }

                currentFiles.add(part);
                currentSize += partSize;

                if (currentSize >= symbolLimit * CHUNK_FILL_THRESHOLD / 100) {
                    chunks.add(createChunk(chunkIndex++, List.copyOf(currentFiles)));
                    currentFiles = new ArrayList<>();
                    currentSize = 0;
                }
            }
        }

        if (!currentFiles.isEmpty()) {
            chunks.add(createChunk(chunkIndex, List.copyOf(currentFiles)));
        }

        return chunks;
    }

    /**
     * Splits file into parts if it exceeds the limit, otherwise returns single-element list.
     */
    private List<FileInfo> splitIfNeeded(FileInfo file, int symbolLimit) {
        if (calculateTotalSize(file) <= symbolLimit) {
            return List.of(file);
        }

        int partLimit = calculatePartLimit(symbolLimit);
        if (partLimit <= 0) {
            log.warn("Symbol limit {} is too small to split file: {}",
                    symbolLimit, file.relativePath());
            throw new IllegalArgumentException(
                    "Symbol limit too small for file: " + file.relativePath());
        }

        return splitIntoParts(file, partLimit);
    }

    private List<FileInfo> splitIntoParts(FileInfo file, int partLimit) {
        String content = file.content();
        int totalParts = (int) Math.ceil((double) content.length() / partLimit);
        List<FileInfo> parts = new ArrayList<>(totalParts);

        for (int i = 0; i < totalParts; i++) {
            int start = i * partLimit;
            int end = Math.min(start + partLimit, content.length());
            parts.add(FileInfo.split(file, content.substring(start, end), i + 1, totalParts));
        }

        log.debug("Split '{}' into {} parts", file.relativePath(), totalParts);
        return parts;
    }

    private Chunk createChunk(int index, List<FileInfo> files) {
        int totalSize = files.stream()
                .mapToInt(this::calculateTotalSize)
                .sum();
        return new Chunk(index, files, totalSize);
    }

    private int calculateTotalSize(FileInfo file) {
        return file.size() + estimateHeaderSize(file);
    }

    private int calculatePartLimit(int symbolLimit) {
        return symbolLimit - HEADER_BASE_SIZE - SPLIT_SAFETY_MARGIN;
    }

    private int estimateHeaderSize(FileInfo file) {
        return HEADER_BASE_SIZE + file.relativePath().toString().length();
    }

    private void logAggregationResult(List<Chunk> chunks) {
        log.info("Aggregation complete. {} chunks created", chunks.size());
        if (log.isDebugEnabled()) {
            for (Chunk chunk : chunks) {
                log.debug("Chunk {}: {} files, {} symbols",
                        chunk.index(), chunk.files().size(), chunk.totalSize());
            }
        }
    }
}