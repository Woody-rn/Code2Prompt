package ru.npepub.service.impl;

import ru.npepub.di.C2PComponent;
import ru.npepub.service.PathResolver;

import java.nio.file.Path;

/**
 * Resolves paths like: {outputDir}/code2prompt_part1.txt
 */

@C2PComponent
class PathResolverImpl implements PathResolver {

    private static final String FILE_PREFIX = "code2prompt_part";
    private static final String FILE_EXTENSION = ".txt";

    @Override
    public Path resolve(Path outputDir, int chunkIndex) {
        return outputDir.resolve(FILE_PREFIX + chunkIndex + FILE_EXTENSION);
    }
}