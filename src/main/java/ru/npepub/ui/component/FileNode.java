package ru.npepub.ui.component;

import java.nio.file.Path;

/**
 * UI model for file tree node.
 * Stores display name and reference to actual file path.
 */

public record FileNode(String name, Path path, boolean isDirectory) {

    @Override
    public String toString() {
        return name;
    }
}