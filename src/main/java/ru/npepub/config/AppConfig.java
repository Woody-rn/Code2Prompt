package ru.npepub.config;

/**
 * Application configuration.
 */
public record AppConfig(
        AiModelConfig aiModel,
        PathConfig paths,
        FilterConfig filter,
        LogConfig log,
        PromptConfig prompt,
        boolean debugMode
) {
    /** Convenience accessor for effective model limit. */
    public int effectiveLimit() {
        return aiModel.effectiveLimit();
    }

    public static AppConfig defaults() {
        return new AppConfig(
                AiModelConfig.defaults(),
                PathConfig.defaults(),
                FilterConfig.defaults(),
                LogConfig.defaults(),
                PromptConfig.defaults(),
                false
        );
    }
}