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

    @C2PInject
    private ConfigPort configPort;

    @Override
    public boolean shouldInclude(Path filePath) {
        AppConfig config = configPort.load();
        Set<String> excluded = new HashSet<>(config.filter().excludedDirs());

        for (Path part : filePath) {
            String name = part.toString();
            if (excluded.contains(name) || excluded.contains(name + "/")) {
                return false;
            }
        }
        return true;
    }
}