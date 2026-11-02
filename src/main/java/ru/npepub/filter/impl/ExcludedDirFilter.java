package ru.npepub.filter.impl;

import ru.npepub.config.FilterConfig;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.filter.ConfigurableFilter;
import ru.npepub.filter.DirectoryFilter;

import java.nio.file.Path;
import java.util.Set;

/**
 * Skips directories whose name matches the excluded set.
 *
 * <p>Used by {@code FileScannerImpl} during tree traversal:
 * returning {@code false} from {@link #shouldEnter(Path)} causes
 * {@code FileVisitResult.SKIP_SUBTREE} — the subtree is not visited at all.</p>
 *
 * <p>Comparison is by the directory's own name component (e.g. {@code "build"}
 * or {@code "build/"}), not by full path. This means any directory with that
 * name at any depth is skipped — including {@code module-a/build/},
 * {@code module-b/build/}, etc. This is intentional: it matches typical
 * Gradle/Maven layouts.</p>
 */
@C2PComponent
class ExcludedDirFilter implements DirectoryFilter, ConfigurableFilter {

    private Set<String> excludedDirs = Set.of();

    @Override
    public boolean shouldEnter(Path dirPath) {
        Path name = dirPath.getFileName();
        if (name == null) {
            // Root path (e.g. "/" or "C:\") — no name component, always enter.
            return true;
        }
        String dirName = name.toString();
        return !excludedDirs.contains(dirName) && !excludedDirs.contains(dirName + "/");
    }

    @Override
    public void reloadConfig(FilterConfig config) {
        this.excludedDirs = config.excludedDirs();
    }
}