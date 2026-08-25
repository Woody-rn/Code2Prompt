package ru.npepub.filter.impl;

import ru.npepub.config.ConfigPort;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.FileFilter;

import java.nio.file.Path;
import java.util.Set;

/**
 * Excludes files with sensitive or unwanted names.
 */
@C2PComponent
class FileNameFilter implements FileFilter, ConfigurableFilter {

    @C2PInject
    private ConfigPort configPort;

    private Set<String> excludedFileNames = Set.of();

    @Override
    public boolean shouldInclude(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return !excludedFileNames.contains(fileName);
    }

    @Override
    public void reloadConfig() {
        this.excludedFileNames = configPort.load().filter().excludedFileNames();
    }
}