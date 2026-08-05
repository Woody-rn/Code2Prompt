package ru.npepub.server;

import ru.npepub.model.ProjectInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * HTTP/HTTPS server interface for serving project context to browser extension.
 *
 * @author Nikitin Ruslan
 * @version 1.0.0
 */
public interface ContextServer {

    /**
     * Starts the server on the specified port.
     *
     * @param port        port to listen on (typically 9090)
     * @param files       list of chunk files to serve
     * @param projectInfo project metadata
     * @throws Exception if server startup fails
     */
    public void start(int port, List<Path> files, ProjectInfo projectInfo) throws Exception;

    /**
     * Stops the server gracefully.
     */
    public void stop();

    /**
     * @return true if the server is currently running
     */
    public boolean isRunning();
}
