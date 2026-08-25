package ru.npepub.config;

import ru.npepub.ai.AssistantConfig;

public record AppConfig(
        ModelLimitConfig aiModel,
        PathConfig paths,
        FilterConfig filter,
        LogConfig log,
        PromptConfig prompt,
        AssistantConfig assistant,
        boolean oneFilePerChunk,
        boolean debugMode
) {
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
                AssistantConfig.defaults(),
                false,
                false
        );
    }

    public AppConfig withPaths(PathConfig newPaths) {
        return new AppConfig(aiModel, newPaths, filter, log, prompt, assistant, oneFilePerChunk, debugMode);
    }

    public AppConfig withPrompt(PromptConfig newPrompt) {
        return new AppConfig(aiModel, paths, filter, log, newPrompt, assistant, oneFilePerChunk, debugMode);
    }

    public AppConfig withFilter(FilterConfig newFilter) {
        return new AppConfig(aiModel, paths, newFilter, log, prompt, assistant, oneFilePerChunk, debugMode);
    }

    public AppConfig withLog(LogConfig newLog) {
        return new AppConfig(aiModel, paths, filter, newLog, prompt, assistant, oneFilePerChunk, debugMode);
    }

    public AppConfig withAssistant(AssistantConfig newAssistant) {
        return new AppConfig(aiModel, paths, filter, log, prompt, newAssistant, oneFilePerChunk, debugMode);
    }

    public AppConfig withDebugMode(boolean newDebugMode) {
        return new AppConfig(aiModel, paths, filter, log, prompt, assistant, oneFilePerChunk, newDebugMode);
    }
}