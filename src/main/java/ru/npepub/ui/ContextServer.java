package ru.npepub.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.model.ProjectInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple HTTP server that serves project context to the browser extension.
 * Endpoints:
 * GET /context        — all parts concatenated
 * GET /context/parts?id=N — specific part by index (0-based)
 * GET /project        — project name as JSON
 */
public class ContextServer {

    private static final Logger log = LoggerFactory.getLogger(ContextServer.class);

    private HttpServer server;
    private List<Path> contextFiles;
    private ProjectInfo projectInfo;

    /**
     * Starts the server on the given port.
     *
     * @param port        the port to listen on
     * @param files       list of chunk files to serve
     * @param projectInfo project identity for the /project endpoint
     */
    public void start(int port, List<Path> files, ProjectInfo projectInfo) throws IOException {
        this.contextFiles = files;
        this.projectInfo = projectInfo;
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/context", this::handleContext);
        server.createContext("/context/parts", this::handleParts);
        server.createContext("/project", this::handleProject);
        server.setExecutor(null);
        server.start();

        log.info("Context server started on port {}", port);
    }

    /**
     * Stops the server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            log.info("Context server stopped");
        }
    }

    /**
     * @return true if the server is currently running
     */
    public boolean isRunning() {
        return server != null;
    }

    /**
     * GET /context — returns all parts concatenated.
     */
    private void handleContext(HttpExchange exchange) throws IOException {
        String json = contextFiles.stream()
                .map(f -> {
                    try {
                        return Files.readString(f);
                    } catch (IOException e) {
                        return "Error reading: " + f.getFileName();
                    }
                })
                .collect(Collectors.joining("\n\n"));

        sendJson(exchange, json);
    }

    /**
     * GET /context/parts?id=N — returns a specific part by index (0-based).
     */
    private void handleParts(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int index = 0;
        if (query != null && query.startsWith("id=")) {
            index = Integer.parseInt(query.substring(3));
        }

        if (index >= 0 && index < contextFiles.size()) {
            String content = Files.readString(contextFiles.get(index));
            String json = String.format(
                    "{\"index\":%d,\"total\":%d,\"content\":\"%s\"}",
                    index + 1,
                    contextFiles.size(),
                    escapeJson(content)
            );
            sendJson(exchange, json);
        } else {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    /**
     * GET /project — returns the project name as JSON.
     */
    private void handleProject(HttpExchange exchange) throws IOException {
        String json = "{\"name\":\"" + (projectInfo != null ? projectInfo.name() : "") + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String escapeJson(String content) {
        return content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}