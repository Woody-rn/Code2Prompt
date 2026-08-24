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
        boolean oneFilePerChunk,
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
                false,
                false
        );
    }

    /** Returns a copy with a new paths config. */
    public AppConfig withPaths(PathConfig newPaths) {
        return new AppConfig(aiModel, newPaths, filter, log, prompt, oneFilePerChunk, debugMode);
    }

    /** Returns a copy with a new prompt config. */
    public AppConfig withPrompt(PromptConfig newPrompt) {
        return new AppConfig(aiModel, paths, filter, log, newPrompt, oneFilePerChunk, debugMode);
    }
}