package ru.npepub.filter.impl;

import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.di.api.C2PPrimary;
import ru.npepub.filter.FileFilter;

import java.nio.file.Path;
import java.util.List;

/**
 * Combines all file filters. Returns true only if all filters pass.
 */
@C2PComponent
@C2PPrimary
class CompositeFileFilter implements FileFilter {

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
}