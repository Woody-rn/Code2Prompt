package ru.npepub.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Implementation of PromptAssistant using Ollama REST API.
 */
@C2PComponent
class PromptAssistantOllama implements PromptAssistant {

    private static final Logger log = LoggerFactory.getLogger(PromptAssistantOllama.class);

    @C2PInject
    private ConfigPort configPort;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String improve(String text) {
        String prompt = "Перепиши этот промпт для AI-модели. Сделай его подробнее, конкретнее и эффективнее. " +
                "Верни ТОЛЬКО готовый текст промпта, без объяснений и комментариев.\n\n" + text;
        return generate(prompt);
    }

    @Override
    public String expand(String text) {
        String prompt = "Разверни эту короткую фразу в полноценный промпт для анализа кода проекта. " +
                "Верни ТОЛЬКО готовый текст промпта, без объяснений и комментариев.\n\n" + text;
        return generate(prompt);
    }

    @Override
    public String generateName(String text) {
        String prompt = "Придумай короткое имя (2-4 слова) для этого шаблона промпта. " +
                "Верни ТОЛЬКО имя, без пояснений.\n\n" + text;
        return generate(prompt);
    }

    @Override
    public boolean isAvailable() {
        try {
            AssistantConfig config = configPort.load().assistant();
            if (!config.enabled()) return false;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.endpoint() + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Assistant is not available: {}", e.getMessage());
            return false;
        }
    }

    private String generate(String prompt) {
        try {
            AssistantConfig config = configPort.load().assistant();
            if (!config.enabled()) {
                return "";
            }

            String json = String.format(
                    "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
                    config.model(),
                    escapeJson(prompt)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.endpoint() + "/api/generate"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Ollama returned status {}", response.statusCode());
                return "";
            }

            return extractResponse(response.body());
        } catch (Exception e) {
            log.error("Failed to generate assistant response", e);
            return "";
        }
    }

    private String extractResponse(String json) {
        int idx = json.indexOf("\"response\"");
        if (idx == -1) return "";
        int start = json.indexOf("\"", idx + 11) + 1;

        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && (end == start || json.charAt(end - 1) != '\\')) {
                break;
            }
            end++;
        }
        if (end >= json.length()) return "";

        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\u003e", ">")
                .replace("\\u003c", "<")
                .replace("\\u0026", "&");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}