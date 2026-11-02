package ru.npepub.filter.impl;

import ru.npepub.config.FilterConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.exception.FilterMisuseException;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.FileFilter;

import java.nio.file.Path;
import java.util.Set;

@C2PComponent
class FileNameFilter implements FileFilter, ConfigurableFilter {

    private Set<String> excludedFileNames = Set.of();

    @Override
    public boolean shouldInclude(Path filePath) {
        Path fileName = filePath.getFileName();
        if (fileName == null) {
            throw new FilterMisuseException(
                    "FileFilter called with non-file path: " + filePath);
        }
        return !excludedFileNames.contains(fileName.toString());
    }

    @Override
    public void reloadConfig(FilterConfig config) {
        this.excludedFileNames = config.excludedFileNames();
    }
}