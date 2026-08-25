package ru.npepub.filter.impl;

import ru.npepub.config.FilterConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.FileFilter;

import java.nio.file.Path;
import java.util.Set;

@C2PComponent
class FileNameFilter implements FileFilter, ConfigurableFilter {

    private Set<String> excludedFileNames = Set.of();

    @Override
    public boolean shouldInclude(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return !excludedFileNames.contains(fileName);
    }

    @Override
    public void reloadConfig(FilterConfig config) {
        this.excludedFileNames = config.excludedFileNames();
    }
}