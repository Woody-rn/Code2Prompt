package ru.npepub.util;

import java.io.File;
import java.nio.file.Path;

/**
 * Resolves output path with project name subfolder.
 */
public class ProjectPathResolver {

    public static String resolve(String sourcePath, String outputPath) {
        String projectName = Path.of(sourcePath).getFileName().toString();
        return outputPath + File.separator + projectName;
    }
}