package ru.npepub.service.impl;

import ru.npepub.config.Code2PromptPaths;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.service.PathResolver;

import java.nio.file.Path;

/**
 * Resolves paths like: {outputDir}/code2prompt_part1.txt
 */
@C2PComponent
class PathResolverImpl implements PathResolver {

    @Override
    public Path resolve(Path outputDir, int chunkIndex) {
        return outputDir.resolve(Code2PromptPaths.CHUNK_FILE_PREFIX + chunkIndex + ".txt");
    }
}