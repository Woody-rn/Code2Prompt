package ru.npepub.server;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.PromptConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.model.ProjectInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes and handles HTTP requests for the built-in HTTPS server.
 *
 * <p>Provides three endpoints for the browser extension and debugging:</p>
 * <ul>
 *   <li><b>GET /context</b> — returns concatenated content of all chunk files as plain text.
 *       Used for quick preview in browser. Not consumed by the extension.</li>
 *   <li><b>GET /context/parts?id=N</b> — returns a single chunk (N is zero-based index)
 *       as JSON: {@code {index, total, content}}. The content includes prompt prefix
 *       resolved from {@link PromptConfig} templates. Consumed by the extension.</li>
 *   <li><b>GET /project</b> — returns project name as JSON: {@code {name}}.
 *       Consumed by the extension to display current project.</li>
 * </ul>
 *
 * <p>State management:</p>
 * <ul>
 *   <li>{@link #updateContext} — called after each scan to refresh files and project info.</li>
 *   <li>{@link #updatePrompt} — called when user edits prompt in UI without rescanning.</li>
 * </ul>
 *
 * <p>Thread safety: state fields are updated from JavaFX thread,
 * but read from the HTTP server thread. This is acceptable because
 * the server is stopped before each new scan, and prompt updates
 * happen between requests.</p>
 */
@C2PComponent
class ContextRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(ContextRequestHandler.class);

    @C2PInject private JsonResponseHelper jsonHelper;

    private List<Path> contextFiles;
    private ProjectInfo projectInfo;
    private PromptConfig promptConfig = PromptConfig.defaults();

    /** Updates the context files and project info. */
    public void updateContext(List<Path> files, ProjectInfo projectInfo, PromptConfig promptConfig) {
        this.contextFiles = files;
        this.projectInfo = projectInfo;
        this.promptConfig = promptConfig;
    }

    /** Updates only the prompt config. */
    public void updatePrompt(PromptConfig promptConfig) {
        this.promptConfig = promptConfig;
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
        jsonHelper.sendRawString(exchange, sb.toString(), 200);
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
        int total = contextFiles.size();
        int partNumber = index + 1;
        boolean isLast = partNumber == total;

        String prefix = isLast
                ? resolveTemplate(promptConfig.finalPartTemplate(), partNumber, total)
                : resolveTemplate(promptConfig.partPrefixTemplate(), partNumber, total);

        if (isLast && !promptConfig.systemPrompt().isBlank()) {
            prefix = prefix + "\n" + promptConfig.systemPrompt() + "\n\n";
        }

        String fullContent = prefix + content;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("index", partNumber);
        response.put("total", total);
        response.put("content", fullContent);

        jsonHelper.sendJson(exchange, response);
    }

    private void handleProject(HttpExchange exchange) throws IOException {
        String name = projectInfo != null ? projectInfo.name() : "";
        jsonHelper.sendJson(exchange, Map.of("name", name));
    }

    private int parseIndexFromQuery(String query) {
        if (query == null) return 0;

        try {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue[0].equals("id") && keyValue.length > 1) {
                    return Integer.parseInt(keyValue[1]);
                }
            }
            return 0;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String resolveTemplate(String template, int part, int total) {
        return template
                .replace("{part}", String.valueOf(part))
                .replace("{total}", String.valueOf(total));
    }
}