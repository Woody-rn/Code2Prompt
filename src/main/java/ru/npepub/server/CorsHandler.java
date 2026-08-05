package ru.npepub.server;

import com.sun.net.httpserver.HttpExchange;
import ru.npepub.di.api.C2PComponent;

/**
 * Handles CORS headers and preflight (OPTIONS) requests.
 */

@C2PComponent
class CorsHandler {

    /**
     * Adds CORS headers to the response.
     */

    public void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, Accept, X-Requested-With, Cache-Control, Pragma, Expires, Authorization");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Access-Control-Allow-Private-Network", "true");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }

    /**
     * Handles OPTIONS preflight request.
     *
     * @return true if this was a preflight request and was handled
     */

    public boolean handlePreflight(HttpExchange exchange) {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            try {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            } catch (Exception e) {
                // Ignore close errors for preflight
            }
            return true;
        }
        return false;
    }
}