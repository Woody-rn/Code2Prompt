package ru.npepub.server;

import com.sun.net.httpserver.HttpExchange;
import ru.npepub.di.api.C2PComponent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Helper for sending JSON responses and escaping JSON strings.
 */

@C2PComponent
class JsonResponseHelper {

    /**
     * Sends a successful JSON response with status 200.
     */

    public void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /**
     * Sends an error response with the specified status code.
     */

    public void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = "{\"error\":\"" + escapeJson(message) + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /**
     * Escapes special characters for JSON output.
     */

    public String escapeJson(String content) {
        return content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }
}