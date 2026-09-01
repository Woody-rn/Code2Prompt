package ru.npepub.server;

import com.sun.net.httpserver.HttpExchange;
import ru.npepub.di.api.C2PComponent;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Helper for sending JSON responses.
 * Owns ObjectMapper and handles HTTP-level concerns.
 */
@C2PComponent
class JsonResponseHelper {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sends a raw string response (no JSON serialization).
     *
     * @param exchange   HTTP exchange
     * @param content    raw content to send
     * @param statusCode HTTP status code
     */
    public void sendRawString(HttpExchange exchange, String content, int statusCode) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /**
     * Sends a successful JSON response with status 200.
     *
     * @param exchange HTTP exchange
     * @param data     object to serialize as JSON
     */
    public void sendJson(HttpExchange exchange, Object data) throws IOException {
        sendJson(exchange, data, 200);
    }

    /**
     * Sends a JSON response with the specified status code.
     *
     * @param exchange   HTTP exchange
     * @param data       object to serialize as JSON
     * @param statusCode HTTP status code
     */
    public void sendJson(HttpExchange exchange, Object data, int statusCode) throws IOException {
        String json = mapper.writeValueAsString(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /**
     * Sends an error response with the specified status code.
     *
     * @param exchange HTTP exchange
     * @param code     HTTP status code
     * @param message  error message
     */
    public void sendError(HttpExchange exchange, int code, String message) throws IOException {
        sendJson(exchange, Map.of("error", message), code);
    }
}