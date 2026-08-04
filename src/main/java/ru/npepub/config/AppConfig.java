package ru.npepub.config;

import java.nio.file.Path;
import java.util.List;

/**
 * Application configuration values.
 */
public record AppConfig(
        String modelName,
        int maxSymbols,
        double safetyMargin,
        Path outputPath,
        LogLevel logLevel,
        boolean errorLogEnabled,
        boolean debugMode,
        List<String> recentProjects,
        int recentProjectsCount,
        List<String> excludedDirs
) {
    public enum LogLevel {
        DEBUG, INFO, WARN, OFF
    }

    public static final String DEFAULT_MODEL = "DeepSeek V3";
    public static final int DEFAULT_MAX_SYMBOLS = 100_000;
    public static final double DEFAULT_SAFETY_MARGIN = 0.05;
    public static final Path DEFAULT_OUTPUT_PATH = Path.of(
            System.getProperty("user.home"), "ContextPack"
    );
    public static final List<String> DEFAULT_EXCLUDED_DIRS = List.of(
            ".git", ".gradle", ".idea", "build", "target",
            "node_modules", "__pycache__", ".svn", "out", "dist"
    );

    /**
     * @return effective symbol limit after applying safety margin
     */
    public int effectiveLimit() {
        return (int) (maxSymbols * (1 - safetyMargin));
    }

    /**
     * @return default configuration
     */
    public static AppConfig defaults() {
        return new AppConfig(
                DEFAULT_MODEL,
                DEFAULT_MAX_SYMBOLS,
                DEFAULT_SAFETY_MARGIN,
                DEFAULT_OUTPUT_PATH,
                LogLevel.INFO,
                true,
                false,
                List.of(),
                10,
                DEFAULT_EXCLUDED_DIRS
        );
    }
}