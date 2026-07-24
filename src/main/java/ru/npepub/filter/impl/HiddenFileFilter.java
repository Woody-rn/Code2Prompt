package ru.npepub.filter.impl;

import ru.npepub.di.api.C2PComponent;
import ru.npepub.filter.FileFilter;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Skips hidden files and directories.
 */
@C2PComponent
class HiddenFileFilter implements FileFilter {

    @Override
    public boolean shouldInclude(Path filePath) {
        try {
            return !Files.isHidden(filePath);
        } catch (Exception e) {
            return true;
        }
    }
}