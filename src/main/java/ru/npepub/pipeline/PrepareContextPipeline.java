package ru.npepub.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;
import ru.npepub.service.FileScanner;
import ru.npepub.service.OutputWriter;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Pipeline that prepares project files as context for AI models.
 * Supports step-by-step execution with progress reporting and cancellation.
 */
@C2PComponent
public class PrepareContextPipeline {

    private static final Logger log = LoggerFactory.getLogger(PrepareContextPipeline.class);

    @C2PInject private FileScanner scanner;
    @C2PInject private FileAggregator aggregator;
    @C2PInject private OutputWriter writer;

    /**
     * Executes the pipeline step by step.
     *
     * @param sourcePath root directory to scan
     * @param outputPath directory for output txt files
     * @param limit      max symbols per chunk
     * @param onProgress callback for progress messages
     * @param cancelled  flag to cancel execution between steps
     * @return list of written file paths, or empty if cancelled
     */
    public List<Path> execute(String sourcePath, String outputPath, int limit,
                              Consumer<String> onProgress, AtomicBoolean cancelled) {
        log.info("Starting pipeline: {} → {}", sourcePath, outputPath);

        onProgress.accept("Сканирование...");
        List<FileInfo> files = scanner.scan(Path.of(sourcePath));
        log.info("Scanned {} files", files.size());
        if (cancelled.get()) return List.of();

        onProgress.accept("Разбивка на части...");
        List<Chunk> chunks = aggregator.aggregate(files, limit);
        log.info("Created {} chunks", chunks.size());
        if (cancelled.get()) return List.of();

        onProgress.accept("Запись файлов...");
        List<Path> result = writer.write(chunks, Path.of(outputPath));
        log.info("Pipeline complete. {} files written", result.size());

        return result;
    }
}