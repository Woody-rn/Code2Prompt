package ru.npepub.config;

/**
 * AI model settings.
 */
public record AiModelConfig(
        String name,
        int maxSymbols,
        double safetyMargin
) {
    /** Returns the effective symbol limit after applying the safety margin. */
    public int effectiveLimit() {
        return (int) (maxSymbols * (1 - safetyMargin));
    }

    public static AiModelConfig defaults() {
        return new AiModelConfig("DeepSeek V3", 100_000, 0.05);
    }
}