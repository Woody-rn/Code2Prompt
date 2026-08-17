package ru.npepub.config;

/**
 * Application configuration.
 */
public record AppConfig(
        ModelLimitConfig aiModel,
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
                ModelLimitConfig.defaults(),
                PathConfig.defaults(),
                FilterConfig.defaults(),
                LogConfig.defaults(),
                PromptConfig.defaults(),
                false
        );
    }
}