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

/**
 * Writes chunks to txt files on disk.
 */

@C2PComponent
class OutputWriterImpl implements OutputWriter {

    private static final Logger log = LoggerFactory.getLogger(OutputWriterImpl.class);

    @C2PInject
    private ChunkFormatter formatter;
    @C2PInject
    private PathResolver pathResolver;

    @Override
    public List<Path> write(List<Chunk> chunks, Path outputDir) {
        log.info("Writing {} chunks to {}", chunks.size(), outputDir);

        createDirectories(outputDir);

        List<Path> createdFiles = new ArrayList<>();
        for (Chunk chunk : chunks) {
            Path outputFile = pathResolver.resolve(outputDir, chunk.index());
            writeToFile(outputFile, chunk);

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

    private void writeToFile(Path filePath, Chunk chunk) {
        String formatted = formatter.format(chunk);
        try {
            Files.writeString(filePath, formatted);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

}