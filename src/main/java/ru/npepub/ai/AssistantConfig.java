package ru.npepub.ai;

/**
 * Configuration for the local AI assistant.
 */
public record AssistantConfig(
        String endpoint,
        String model,
        boolean enabled
) {
    public static AssistantConfig defaults() {
        return new AssistantConfig("http://localhost:11434", "qwen3.5:latest", true);
    }
}