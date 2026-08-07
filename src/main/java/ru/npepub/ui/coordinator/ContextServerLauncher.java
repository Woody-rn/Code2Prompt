package ru.npepub.ui.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.model.ProjectInfo;
import ru.npepub.server.ContextServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controls the lifecycle of the HTTPS context server.
 */
@C2PComponent
public class ContextServerLauncher {

    private static final Logger log = LoggerFactory.getLogger(ContextServerLauncher.class);

    @C2PInject
    private ContextServer contextServer;

    /** Starts the server serving chunk files from the output directory. */
    public void start(Path outputDir, ProjectInfo projectInfo) throws Exception {
        List<Path> files = Files.list(outputDir)
                .filter(f -> f.getFileName().toString().startsWith("code2prompt_part"))
                .sorted()
                .collect(Collectors.toList());

        contextServer.start(9090, files, projectInfo);
    }

    /** Stops the running server. */
    public void stop() {
        contextServer.stop();
    }

    /** Returns whether the server is currently running. */
    public boolean isRunning() {
        return contextServer.isRunning();
    }
}