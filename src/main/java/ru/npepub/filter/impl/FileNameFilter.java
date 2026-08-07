package ru.npepub.filter.impl;

import ru.npepub.config.AppConfig;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.filter.FileFilter;

import java.nio.file.Path;

/**
 * Excludes files with sensitive or unwanted names.
 */
@C2PComponent
class FileNameFilter implements FileFilter {

    @C2PInject
    private ConfigPort configPort;

    @Override
    public boolean shouldInclude(Path filePath) {
        AppConfig config = configPort.load();
        String fileName = filePath.getFileName().toString();
        return !config.filter().excludedFileNames().contains(fileName);
    }
}