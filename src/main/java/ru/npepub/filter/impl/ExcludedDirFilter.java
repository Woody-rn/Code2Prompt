package ru.npepub.filter.impl;

import ru.npepub.config.AppConfig;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.filter.FileFilter;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Excludes files inside user-configured directories from scanning.
 */
@C2PComponent
class ExcludedDirFilter implements FileFilter {

    private static final boolean EXCLUDE = false;
    private static final boolean INCLUDE = true;

    @C2PInject
    private ConfigPort configPort;

    @Override
    public boolean shouldInclude(Path filePath) {
        AppConfig config = configPort.load();
        Set<String> excluded = new HashSet<>(config.excludedDirs());

        for (Path part : filePath) {
            if (excluded.contains(part.toString())) {
                return EXCLUDE;
            }
        }
        return INCLUDE;
    }
}