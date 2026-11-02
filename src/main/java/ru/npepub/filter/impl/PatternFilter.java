package ru.npepub.filter.impl;

import ru.npepub.config.FilterConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.exception.FilterMisuseException;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.FileFilter;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

@C2PComponent
class PatternFilter implements FileFilter, ConfigurableFilter {

    private List<PathMatcher> matchers = List.of();

    @Override
    public boolean shouldInclude(Path filePath) {
        Path fileName = filePath.getFileName();
        if (fileName == null) {
            throw new FilterMisuseException(
                    "FileFilter called with non-file path: " + filePath);
        }
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(fileName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reloadConfig(FilterConfig config) {
        this.matchers = config.patterns().stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .toList();
    }
}