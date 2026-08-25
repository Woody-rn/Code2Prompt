package ru.npepub.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.model.Chunk;
import ru.npepub.service.ChunkFormatter;
import ru.npepub.service.OutputWriter;
import ru.npepub.service.PathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Writes chunks to txt files on disk.
 */
@C2PComponent
class OutputWriterImpl implements OutputWriter {

    private static final Logger log = LoggerFactory.getLogger(OutputWriterImpl.class);
    private static final String FILE_PREFIX = "code2prompt_part";

    @C2PInject
    private ChunkFormatter formatter;
    @C2PInject
    private PathResolver pathResolver;

    @Override
    public List<Path> write(List<Chunk> chunks, Path outputDir, String separator) {
        log.info("Writing {} chunks to {}", chunks.size(), outputDir);

        createDirectories(outputDir);
        cleanOldChunks(outputDir);

        List<Path> createdFiles = new ArrayList<>();
        for (Chunk chunk : chunks) {
            Path outputFile = pathResolver.resolve(outputDir, chunk.index());
            writeToFile(outputFile, chunk, separator);
            log.info("Written: {} ({} symbols)", outputFile.getFileName(), chunk.totalSize());
            createdFiles.add(outputFile);
        }

        log.info("All chunks written. {} files created", createdFiles.size());
        return createdFiles;
    }

    private void createDirectories(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory: " + outputDir, e);
        }
    }

    /**
     * Deletes all old chunk files from the output directory before writing new ones.
     * Only files matching the chunk prefix are removed; other files are untouched.
     */
    private void cleanOldChunks(Path outputDir) {
        try (Stream<Path> files = Files.list(outputDir)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(FILE_PREFIX))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                            log.debug("Deleted old chunk: {}", p.getFileName());
                        } catch (IOException e) {
                            log.warn("Failed to delete old chunk: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to clean output directory: {}", outputDir, e);
        }
    }

    private void writeToFile(Path filePath, Chunk chunk, String separator) {
        String formatted = formatter.format(chunk, separator);
        try {
            Files.writeString(filePath, formatted);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }
}