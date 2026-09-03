package ru.npepub.ui.task;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Runs a background task in a separate thread with cancellation support.
 * Reports progress via UI callbacks on the JavaFX thread.
 */
public class BackgroundTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(BackgroundTaskExecutor.class);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Runs the given task in a background thread.
     *
     * @param task          the task to execute
     * @param onProgress    called with progress messages (JavaFX thread)
     * @param onEachResult  called for each result path (JavaFX thread)
     * @param onComplete    called when finished or cancelled (JavaFX thread)
     */
    public void run(BackgroundTask task,
                    Consumer<String> onProgress,
                    Consumer<Path> onEachResult,
                    Runnable onComplete) {
        cancelled.set(false);
        new Thread(() -> {
            try {
                List<Path> results = task.execute(
                        msg -> Platform.runLater(() -> onProgress.accept(msg)),
                        cancelled
                );

                Platform.runLater(() -> {
                    if (!cancelled.get()) {
                        results.forEach(onEachResult);
                    }
                    onComplete.run();
                });
            } catch (Exception e) {
                log.error("Task failed", e);
                Platform.runLater(() -> {
                    onProgress.accept("Ошибка: " + e.getMessage());
                    onComplete.run();
                });
            }
        }).start();
    }

    /**
     * Requests cancellation of the running task.
     */
    public void cancel() {
        cancelled.set(true);
    }
}