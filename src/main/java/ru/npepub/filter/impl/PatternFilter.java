package ru.npepub.filter.impl;

import ru.npepub.config.AppConfig;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.filter.FileFilter;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Excludes files matching glob patterns (e.g. *.class, Test*.java).
 */
@C2PComponent
class PatternFilter implements FileFilter {

    @C2PInject
    private ConfigPort configPort;

    @Override
    public boolean shouldInclude(Path filePath) {
        AppConfig config = configPort.load();
        Path fileName = filePath.getFileName();

        for (String pattern : config.filter().patterns()) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            if (matcher.matches(fileName)) {
                return false;
            }
        }
        return true;
    }
}