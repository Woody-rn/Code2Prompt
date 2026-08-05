package ru.npepub.server;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.model.ProjectInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all HTTP requests for the context server.
 * Routes: /context, /context/parts, /project
 */

@C2PComponent
class ContextRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ContextRequestHandler.class);

    @C2PInject private JsonResponseHelper jsonHelper;
    private List<Path> contextFiles;
    private ProjectInfo projectInfo;

    /**
     * Updates the context files and project info.
     */

    public void updateContext(List<Path> files, ProjectInfo projectInfo) {
        this.contextFiles = files;
        this.projectInfo = projectInfo;
    }

    /**
     * Main request handler. Routes to appropriate handler based on path.
     */

    public void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        log.debug("Request: {} {}", method, path);

        if (!"GET".equals(method)) {
            jsonHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }

        switch (path) {
            case "/context":
                handleContext(exchange);
                break;
            case "/context/parts":
                handleParts(exchange);
                break;
            case "/project":
                handleProject(exchange);
                break;
            default:
                jsonHelper.sendError(exchange, 404, "Not found");
        }
    }

    private void handleContext(HttpExchange exchange) throws IOException {
        if (contextFiles == null || contextFiles.isEmpty()) {
            jsonHelper.sendError(exchange, 404, "No files available. Please run scan first.");
            return;
        }

        String content = contextFiles.stream()
                .map(f -> {
                    try {
                        return Files.readString(f);
                    } catch (IOException e) {
                        return "Error reading: " + f.getFileName();
                    }
                })
                .collect(Collectors.joining("\n\n"));

        jsonHelper.sendJson(exchange, content);
    }

    private void handleParts(HttpExchange exchange) throws IOException {
        if (contextFiles == null || contextFiles.isEmpty()) {
            jsonHelper.sendError(exchange, 404, "No files available. Please run scan first.");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        log.debug("Parts request. Query: {}, Total files: {}", query, contextFiles.size());

        int index = parseIndexFromQuery(query);

        if (index >= 0 && index < contextFiles.size()) {
            try {
                String content = Files.readString(contextFiles.get(index));
                String json = String.format(
                        "{\"index\":%d,\"total\":%d,\"content\":\"%s\"}",
                        index + 1,
                        contextFiles.size(),
                        jsonHelper.escapeJson(content)
                );
                jsonHelper.sendJson(exchange, json);
            } catch (IOException e) {
                log.error("Failed to read file at index {}", index, e);
                jsonHelper.sendError(exchange, 500, "Failed to read file: " + e.getMessage());
            }
        } else {
            log.warn("Index {} out of range (0-{})", index, contextFiles.size() - 1);
            jsonHelper.sendError(exchange, 404,
                    "Part not found: " + index + ". Total: " + contextFiles.size());
        }
    }

    private int parseIndexFromQuery(String query) {
        if (query == null || !query.startsWith("id=")) {
            log.warn("No valid id parameter in request, using default 0");
            return 0;
        }

        try {
            String idValue = query.substring(3);
            int ampIndex = idValue.indexOf('&');
            if (ampIndex > 0) {
                idValue = idValue.substring(0, ampIndex);
            }
            int index = Integer.parseInt(idValue);
            log.debug("Parsed index: {}", index);
            return index;
        } catch (NumberFormatException e) {
            log.warn("Invalid id parameter: {}", query);
            return -1;
        }
    }

    private void handleProject(HttpExchange exchange) throws IOException {
        String json = "{\"name\":\"" +
                (projectInfo != null ? projectInfo.name() : "") + "\"}";
        jsonHelper.sendJson(exchange, json);
    }
}