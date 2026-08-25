package ru.npepub.filter.impl;

import ru.npepub.config.FilterConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.di.api.C2PPrimary;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.FileFilter;

import java.nio.file.Path;
import java.util.List;

@C2PComponent
@C2PPrimary
class CompositeFileFilter implements FileFilter, ConfigurableFilter {

    @C2PInject
    private List<FileFilter> filters;

    @Override
    public boolean shouldInclude(Path filePath) {
        for (FileFilter filter : filters) {
            if (!filter.shouldInclude(filePath)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reloadConfig(FilterConfig config) {
        for (FileFilter filter : filters) {
            if (filter instanceof ConfigurableFilter configurable) {
                configurable.reloadConfig(config);
            }
        }
    }
}