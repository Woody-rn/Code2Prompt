package ru.npepub.filter.impl;

import ru.npepub.config.FilterConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.FileFilter;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Set;

@C2PComponent
class PatternFilter implements FileFilter, ConfigurableFilter {

    private Set<String> patterns = Set.of();

    @Override
    public boolean shouldInclude(Path filePath) {
        Path fileName = filePath.getFileName();
        for (String pattern : patterns) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            if (matcher.matches(fileName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reloadConfig(FilterConfig config) {
        this.patterns = config.patterns();
    }
}