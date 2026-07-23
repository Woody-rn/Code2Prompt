package ru.npepub.ui.task;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;
import ru.npepub.service.FileScanner;
import ru.npepub.service.OutputWriter;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Background task that scans, aggregates, and writes files.
 * Runs on a separate thread, reports progress via callbacks on the JavaFX thread.
 */
public class ScanTask {

    private static final Logger log = LoggerFactory.getLogger(ScanTask.class);

    private final FileScanner scanner;
    private final FileAggregator aggregator;
    private final OutputWriter writer;
    private final String sourcePath;
    private final String outputPath;
    private final int limit;
    private final Consumer<String> onStatus;
    private final Consumer<Path> onFileWritten;
    private final Runnable onComplete;

    public ScanTask(FileScanner scanner, FileAggregator aggregator, OutputWriter writer,
                    String sourcePath, String outputPath, int limit,
                    Consumer<String> onStatus, Consumer<Path> onFileWritten, Runnable onComplete) {
        this.scanner = scanner;
        this.aggregator = aggregator;
        this.writer = writer;
        this.sourcePath = sourcePath;
        this.outputPath = outputPath;
        this.limit = limit;
        this.onStatus = onStatus;
        this.onFileWritten = onFileWritten;
        this.onComplete = onComplete;
    }

    /**
     * Starts the scan-aggregate-write pipeline on a new thread.
     */
    public void start() {
        new Thread(() -> {
            try {
                status("Сканирование...");
                List<FileInfo> files = scanner.scan(Path.of(sourcePath));
                log.info("Found {} files", files.size());

                status("Разбивка на части...");
                List<Chunk> chunks = aggregator.aggregate(files, limit);

                status("Запись файлов...");
                List<Path> writtenFiles = writer.write(chunks, Path.of(outputPath));

                Platform.runLater(() -> {
                    writtenFiles.forEach(onFileWritten);
                    onComplete.run();
                    status("Готово. Создано " + writtenFiles.size() + " файлов.");
                });
            } catch (Exception e) {
                log.error("Scan failed", e);
                Platform.runLater(() -> {
                    onComplete.run();
                    status("Ошибка: " + e.getMessage());
                });
            }
        }).start();
    }

    private void status(String text) {
        Platform.runLater(() -> onStatus.accept(text));
    }
}