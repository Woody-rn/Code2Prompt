package ru.npepub.config;

import java.nio.file.Path;

/**
 * Output path settings.
 */
public record PathConfig(Path outputPath) {
    public static PathConfig defaults() {
        return new PathConfig(Path.of(System.getProperty("user.home"), "ContextPack"));
    }
}