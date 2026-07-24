package ru.npepub.filter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.filter.FileFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Accepts only valid UTF-8 text files.
 */
@C2PComponent
class TextFileFilter implements FileFilter {

    private static final Logger log = LoggerFactory.getLogger(TextFileFilter.class);

    private static final int PROBE_SIZE = 1024;
    private static final boolean INCLUDE = true;
    private static final boolean EXCLUDE = false;

    @Override
    public boolean shouldInclude(Path filePath) {
        Optional<byte[]> bytes = readBytes(filePath);
        if (bytes.isEmpty()) return EXCLUDE;

        byte[] data = bytes.get();
        if (data.length == 0) return INCLUDE;

        if (containsNullBytes(data)) {
            log.debug("Skipping binary file: {}", filePath);
            return EXCLUDE;
        }
        return INCLUDE;
    }

    private Optional<byte[]> readBytes(Path filePath) {
        try {
            return Optional.of(Files.readAllBytes(filePath));
        } catch (IOException e) {
            log.debug("Skipping unreadable file: {}", filePath);
            return Optional.empty();
        }
    }

    private boolean containsNullBytes(byte[] bytes) {
        int limit = Math.min(bytes.length, PROBE_SIZE);
        for (int i = 0; i < limit; i++) {
            if (bytes[i] == 0) return true;
        }
        return false;
    }
}