package ru.npepub.config;

import java.nio.file.Path;

/**
 * Centralized file system locations for Code2Prompt.
 */
public final class Code2PromptPaths {

    public static final Path HOME_DIR = Path.of(System.getProperty("user.home"), ".code2prompt");
    public static final Path CONFIG_DIR = HOME_DIR.resolve("config");
    public static final Path CONFIG_FILE = CONFIG_DIR.resolve("app.json");
    public static final Path HISTORY_FILE = HOME_DIR.resolve("history.properties");
    public static final Path LOG_DIR = HOME_DIR.resolve("logs");
    public static final Path KEYSTORE_FILE = HOME_DIR.resolve("keystore.jks");
    public static final String CHUNK_FILE_PREFIX = "code2prompt_part";

    private Code2PromptPaths() {
    }
}