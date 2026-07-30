package ru.npepub.model;

import java.nio.file.Path;

public record ProjectInfo(String name) {

    public static ProjectInfo from(String sourcePath) {
        return new ProjectInfo(Path.of(sourcePath).getFileName().toString());
    }
}