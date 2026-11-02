package ru.npepub.filter;

import java.nio.file.Path;

/**
 * Decides whether a directory should be entered during traversal.
 *
 * <p>Unlike {@link FileFilter}, which answers "include this file?",
 * this filter controls the traversal itself: if {@link #shouldEnter(Path)}
 * returns {@code false}, the whole subtree is skipped
 * ({@code FileVisitResult.SKIP_SUBTREE}).</p>
 *
 * <p>Called from {@code FileScannerImpl} in {@code preVisitDirectory}.
 * Implementations must be side-effect free.</p>
 */
public interface DirectoryFilter {

    /**
     * @param dirPath directory about to be visited (never the scan root —
     *                the root is always entered)
     * @return true if traversal should enter the directory
     */
    boolean shouldEnter(Path dirPath);
}