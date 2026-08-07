package ru.npepub.config;

/**
 * Logging settings.
 */
public record LogConfig(
        LogLevel level,
        boolean errorEnabled
) {
    public enum LogLevel { DEBUG, INFO, WARN, OFF }

    public static LogConfig defaults() {
        return new LogConfig(LogLevel.INFO, true);
    }
}