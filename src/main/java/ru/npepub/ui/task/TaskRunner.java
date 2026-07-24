package ru.npepub.ui.task;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Runs a pipeline in a background thread with cancellation support.
 * Reports progress via UI callbacks on the JavaFX thread.
 */
public class TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Runs the given action in a background thread.
     *
     * @param action        the blocking action with progress and cancellation support
     * @param onProgress    called with progress messages (JavaFX thread)
     * @param onEachResult  called for each result path (JavaFX thread)
     * @param onComplete    called when finished or cancelled (JavaFX thread)
     */
    public void run(TaskAction action,
                    Consumer<String> onProgress,
                    Consumer<Path> onEachResult,
                    Runnable onComplete) {
        new Thread(() -> {
            try {
                List<Path> results = action.execute(
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

    /**
     * A background action with progress reporting and cancellation support.
     */
    public interface TaskAction {
        List<Path> execute(Consumer<String> onProgress, AtomicBoolean cancelled) throws Exception;
    }
}