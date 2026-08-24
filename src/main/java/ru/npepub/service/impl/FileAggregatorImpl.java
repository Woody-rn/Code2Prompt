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

    @Override
    public List<Chunk> aggregate(List<FileInfo> files, int symbolLimit, boolean oneFilePerChunk) {
        log.info("Aggregating {} files with limit {} symbols, oneFilePerChunk={}",
                files.size(), symbolLimit, oneFilePerChunk);

        if (files.isEmpty()) {
            log.info("No files to aggregate");
            return List.of();
        }

        if (oneFilePerChunk) {
            return aggregateEachFileSeparately(files, symbolLimit);
        }

        return aggregateByLimit(files, symbolLimit);
    }

    private List<Chunk> aggregateEachFileSeparately(List<FileInfo> files, int symbolLimit) {
        List<Chunk> chunks = new ArrayList<>();
        int chunkIndex = 1;

        for (FileInfo file : files) {
            int headerSize = estimateHeaderSize(file);
            int totalFileSize = file.size() + headerSize;

            if (totalFileSize > symbolLimit) {
                log.info("File '{}' ({} symbols) exceeds limit, splitting into parts",
                        file.relativePath(), totalFileSize);

                List<FileInfo> parts = splitFile(file, symbolLimit, headerSize);
                for (FileInfo part : parts) {
                    chunks.add(new Chunk(chunkIndex++, List.of(part),
                            part.size() + estimateHeaderSize(part)));
                }
            } else {
                chunks.add(new Chunk(chunkIndex++, List.of(file), totalFileSize));
            }
        }

        logAggregationResult(chunks);
        return chunks;
    }

    private List<Chunk> aggregateByLimit(List<FileInfo> files, int symbolLimit) {
        List<Chunk> chunks = new ArrayList<>();
        List<FileInfo> currentFiles = new ArrayList<>();
        int currentSize = 0;
        int chunkIndex = 1;

        for (FileInfo file : files) {
            int headerSize = estimateHeaderSize(file);
            int totalFileSize = file.size() + headerSize;

            if (totalFileSize > symbolLimit) {
                log.info("File '{}' ({} symbols) exceeds limit, splitting into parts",
                        file.relativePath(), totalFileSize);

                if (!currentFiles.isEmpty()) {
                    chunks.add(new Chunk(chunkIndex++, List.copyOf(currentFiles), currentSize));
                    currentFiles.clear();
                    currentSize = 0;
                }

                List<FileInfo> parts = splitFile(file, symbolLimit, headerSize);
                for (FileInfo part : parts) {
                    int partSize = part.size() + estimateHeaderSize(part);
                    currentFiles.add(part);
                    currentSize += partSize;

                    if (currentSize >= symbolLimit * 0.9) {
                        chunks.add(new Chunk(chunkIndex++, List.copyOf(currentFiles), currentSize));
                        currentFiles.clear();
                        currentSize = 0;
                    }
                }
                continue;
            }

            if (!canFitInCurrentChunk(currentFiles, currentSize, totalFileSize, symbolLimit)) {
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

        logAggregationResult(chunks);
        return chunks;
    }

    private List<FileInfo> splitFile(FileInfo file, int symbolLimit, int headerSize) {
        int partLimit = symbolLimit - headerSize - 100;
        if (partLimit <= 0) {
            log.warn("Part limit is too small for file: {}", file.relativePath());
            return List.of(file);
        }

        List<FileInfo> parts = new ArrayList<>();
        String content = file.content();
        int totalParts = (int) Math.ceil((double) content.length() / partLimit);

        for (int i = 0; i < totalParts; i++) {
            int start = i * partLimit;
            int end = Math.min(start + partLimit, content.length());
            String partContent = content.substring(start, end);
            parts.add(FileInfo.split(file, partContent, i + 1, totalParts));
        }

        log.debug("Split '{}' into {} parts", file.relativePath(), totalParts);
        return parts;
    }

    private boolean canFitInCurrentChunk(List<FileInfo> currentFiles,
                                         int currentSize,
                                         int totalFileSize,
                                         int symbolLimit) {
        if (currentFiles.isEmpty()) return true;
        return currentSize + totalFileSize <= symbolLimit;
    }

    private int estimateHeaderSize(FileInfo file) {
        return HEADER_BASE_SIZE + file.relativePath().toString().length();
    }

    private void logAggregationResult(List<Chunk> chunks) {
        log.info("Aggregation complete. {} chunks created", chunks.size());
        for (Chunk chunk : chunks) {
            log.debug("Chunk {}: {} files, {} symbols",
                    chunk.index(), chunk.files().size(), chunk.totalSize());
        }
    }
}