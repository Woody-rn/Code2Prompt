package ru.npepub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@C2PComponent
class ConfigPortImpl implements ConfigPort {

    private static final Logger log = LoggerFactory.getLogger(ConfigPortImpl.class);

    private static final Path CONFIG_FILE = Code2PromptPaths.CONFIG_FILE;

    private final ObjectMapper mapper = new ObjectMapper()
            .rebuild()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    @Override
    public AppConfig load() {
        if (!Files.exists(CONFIG_FILE)) {
            log.info("Config file not found, using defaults");
            return AppConfig.defaults();
        }

        log.debug("Loading config from {}", CONFIG_FILE);

        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            return mapper.readValue(in, AppConfig.class);
        } catch (IOException e) {
            log.warn("Failed to load config, using defaults", e);
            return AppConfig.defaults();
        }
    }

    @Override
    public void save(AppConfig config) {
        log.debug("Saving config to {}", CONFIG_FILE);

        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                mapper.writeValue(out, config);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + CONFIG_FILE, e);
        }
    }
}