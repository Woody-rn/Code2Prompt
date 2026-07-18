package ru.npepub.service;

import ru.npepub.model.FileInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * Scans a directory recursively and collects all files.
 */
public interface FileScanner {

    /**
     * @param rootDir the root directory to scan
     * @return list of file infos with relative paths and content
     */
    List<FileInfo> scan(Path rootDir);
}