package ru.npepub.util;

import ru.npepub.model.ProjectInfo;

import java.io.File;

/**
 * Resolves output path with project name subfolder.
 */
public class ProjectPathResolver {

    public static String resolveOutputPath(ProjectInfo project, String baseOutputPath) {
        return baseOutputPath + File.separator + project.name();
    }
}