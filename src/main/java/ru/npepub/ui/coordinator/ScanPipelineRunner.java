package ru.npepub.ui.coordinator;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.pipeline.PrepareContextPipeline;
import ru.npepub.ui.task.TaskRunner;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Executes the scan pipeline with progress reporting and cancellation.
 */
@C2PComponent
public class ScanPipelineRunner {

    private static final Logger log = LoggerFactory.getLogger(ScanPipelineRunner.class);

    @C2PInject
    private PrepareContextPipeline pipeline;

    private final TaskRunner taskRunner = new TaskRunner();
    private List<Path> lastResults;

    /** Runs the pipeline for the given request. */
    public void run(PrepareRequest request,
                    Consumer<String> onProgress,
                    Consumer<Path> onEachResult,
                    Runnable onComplete,
                    Consumer<List<FileInfo>> onFilesScanned,
                    Consumer<String> onStatus) {

        taskRunner.run(
                (progressCallback, cancelled) -> executePipeline(request, progressCallback, cancelled, onFilesScanned, onStatus),
                onProgress,
                onEachResult,
                onComplete
        );
    }

    /** Cancels the running pipeline. */
    public void cancel() {
        taskRunner.cancel();
    }

    /** Returns the paths of the last successful run. */
    public List<Path> getLastResults() {
        return lastResults != null ? List.copyOf(lastResults) : List.of();
    }

    private List<Path> executePipeline(PrepareRequest request,
                                       Consumer<String> onProgress,
                                       AtomicBoolean cancelled,
                                       Consumer<List<FileInfo>> onFilesScanned,
                                       Consumer<String> onStatus) {
        List<FileInfo> files = pipeline.scan(request);
        if (cancelled.get()) return List.of();
        Platform.runLater(() -> onFilesScanned.accept(files));

        onProgress.accept("Разбивка на части...");
        List<Chunk> chunks = pipeline.aggregate(request, files);
        if (cancelled.get()) return List.of();

        onProgress.accept("Запись файлов...");
        List<Path> results = pipeline.write(request, chunks);
        lastResults = results;

        Platform.runLater(() -> onStatus.accept("Готово. Создано " + results.size() + " файлов."));
        return results;
    }
}