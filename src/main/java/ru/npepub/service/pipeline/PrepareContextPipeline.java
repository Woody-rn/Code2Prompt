package ru.npepub.service.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;
import ru.npepub.service.FileScanner;
import ru.npepub.service.OutputWriter;

import java.nio.file.Path;
import java.util.List;

/**
 * Pipeline that prepares project files as context for AI models.
 * Provides three independent steps: scan, aggregate, write.
 */
@C2PComponent
public class PrepareContextPipeline {

    private static final Logger log = LoggerFactory.getLogger(PrepareContextPipeline.class);

    @C2PInject private FileScanner scanner;
    @C2PInject private FileAggregator aggregator;
    @C2PInject private OutputWriter writer;

    /**
     * Scans the source directory and returns found files.
     */
    public List<FileInfo> scan(PrepareRequest request) {
        log.info("Scanning: {}", request.sourcePath());
        List<FileInfo> files = scanner.scan(Path.of(request.sourcePath()));
        log.info("Scanned {} files", files.size());
        return files;
    }

    /**
     * Aggregates files into chunks respecting the symbol limit.
     */
    public List<Chunk> aggregate(PrepareRequest request, List<FileInfo> files) {
        int limit = Integer.parseInt(request.limitText());
        log.info("Aggregating {} files with limit {}", files.size(), limit);
        List<Chunk> chunks = aggregator.aggregate(files, limit);
        log.info("Created {} chunks", chunks.size());
        return chunks;
    }

    /**
     * Writes chunks to txt files in the output directory.
     */
    public List<Path> write(PrepareRequest request, List<Chunk> chunks) {
        log.info("Writing {} chunks to {}", chunks.size(), request.outputPath());
        List<Path> result = writer.write(chunks, Path.of(request.outputPath()));
        log.info("Written {} files", result.size());
        return result;
    }
}