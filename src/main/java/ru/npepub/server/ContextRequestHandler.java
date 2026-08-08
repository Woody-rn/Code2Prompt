package ru.npepub.server;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.config.PromptConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.model.ProjectInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Handles all HTTP requests for the context server.
 * Routes: /context, /context/parts, /project
 */
@C2PComponent
class ContextRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(ContextRequestHandler.class);

    @C2PInject private JsonResponseHelper jsonHelper;
    @C2PInject private ConfigPort configPort;

    private List<Path> contextFiles;
    private ProjectInfo projectInfo;

    /** Updates the context files and project info. */
    public void updateContext(List<Path> files, ProjectInfo projectInfo) {
        this.contextFiles = files;
        this.projectInfo = projectInfo;
    }

    /** Main request handler. Routes to appropriate handler based on path. */
    public void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        log.debug("Request: {} {}", method, path);

        if (!"GET".equals(method)) {
            jsonHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }

        switch (path) {
            case "/context" -> handleContext(exchange);
            case "/context/parts" -> handleParts(exchange);
            case "/project" -> handleProject(exchange);
            default -> jsonHelper.sendError(exchange, 404, "Not found");
        }
    }

    private void handleContext(HttpExchange exchange) throws IOException {
        if (contextFiles == null || contextFiles.isEmpty()) {
            jsonHelper.sendError(exchange, 404, "No files available. Please run scan first.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Path file : contextFiles) {
            sb.append(Files.readString(file)).append("\n\n");
        }
        jsonHelper.sendJson(exchange, sb.toString());
    }

    private void handleParts(HttpExchange exchange) throws IOException {
        if (contextFiles == null || contextFiles.isEmpty()) {
            jsonHelper.sendError(exchange, 404, "No files available. Please run scan first.");
            return;
        }

        int index = parseIndexFromQuery(exchange.getRequestURI().getQuery());
        if (index < 0 || index >= contextFiles.size()) {
            jsonHelper.sendError(exchange, 404, "Part not found: " + index);
            return;
        }

        String content = Files.readString(contextFiles.get(index));
        PromptConfig prompt = configPort.load().prompt();
        int total = contextFiles.size();
        int partNumber = index + 1;
        boolean isLast = partNumber == total;

        String prefix = isLast
                ? resolveTemplate(prompt.finalPartTemplate(), partNumber, total)
                : resolveTemplate(prompt.partPrefixTemplate(), partNumber, total);

        // System prompt добавляется только к последней части, перед содержимым
        if (isLast && !prompt.systemPrompt().isBlank()) {
            prefix = prefix + "\n" + prompt.systemPrompt() + "\n\n";
        }

        String fullContent = prefix + content;

        String json = String.format(
                "{\"index\":%d,\"total\":%d,\"content\":\"%s\"}",
                partNumber, total, jsonHelper.escapeJson(fullContent)
        );
        jsonHelper.sendJson(exchange, json);
    }

    private int parseIndexFromQuery(String query) {
        if (query == null || !query.startsWith("id=")) return 0;
        try {
            String idValue = query.substring(3);
            int ampIndex = idValue.indexOf('&');
            if (ampIndex > 0) idValue = idValue.substring(0, ampIndex);
            return Integer.parseInt(idValue);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void handleProject(HttpExchange exchange) throws IOException {
        String json = "{\"name\":\"" +
                (projectInfo != null ? projectInfo.name() : "") + "\"}";
        jsonHelper.sendJson(exchange, json);
    }

    private String resolveTemplate(String template, int part, int total) {
        return template
                .replace("{part}", String.valueOf(part))
                .replace("{total}", String.valueOf(total));
    }
}