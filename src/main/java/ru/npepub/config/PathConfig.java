package ru.npepub.config;

import java.nio.file.Path;
import java.util.List;

/**
 * Output path and recent project settings.
 */
public record PathConfig(
        Path outputPath,
        List<String> recentProjects,
        int recentProjectsCount
) {
    public static PathConfig defaults() {
        return new PathConfig(
                Path.of(System.getProperty("user.home"), "ContextPack"),
                List.of(),
                10
        );
    }
}