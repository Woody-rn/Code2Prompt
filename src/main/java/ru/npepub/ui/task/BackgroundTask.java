package ru.npepub.ui.task;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A long-running background operation with progress reporting and cancellation.
 * Implementations are executed by {@link BackgroundTaskExecutor} in a separate thread.
 */
public interface BackgroundTask {

    /**
     * Executes the task in a background thread.
     *
     * @param onProgress callback for progress messages (called from background thread,
     *                   implementations should not call it after cancellation)
     * @param cancelled  flag to check for cancellation requests
     * @return list of created files
     * @throws Exception if task fails
     */
    List<Path> execute(Consumer<String> onProgress, AtomicBoolean cancelled) throws Exception;
}