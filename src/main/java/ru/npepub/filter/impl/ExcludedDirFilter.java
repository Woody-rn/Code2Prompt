package ru.npepub.filter.impl;

import ru.npepub.config.FilterConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.FileFilter;

import java.nio.file.Path;
import java.util.Set;

@C2PComponent
class ExcludedDirFilter implements FileFilter, ConfigurableFilter {

    private Set<String> excludedDirs = Set.of();

    @Override
    public boolean shouldInclude(Path filePath) {
        for (Path part : filePath) {
            String name = part.toString();
            if (excludedDirs.contains(name) || excludedDirs.contains(name + "/")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reloadConfig(FilterConfig config) {
        this.excludedDirs = config.excludedDirs();
    }
}