package ru.npepub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * File-based implementation of {@link ConfigPort}.
 * Stores configuration in ~/.code2prompt/config/app.properties.
 */

@C2PComponent
class ConfigPortImpl implements ConfigPort {

    private static final Logger log = LoggerFactory.getLogger(ConfigPortImpl.class);

    private static final Path CONFIG_DIR = Path.of(
            System.getProperty("user.home"), ".code2prompt", "config"
    );
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("app.properties");

    @Override
    public AppConfig load() {
        if (!Files.exists(CONFIG_FILE)) {
            log.info("Config file not found, using defaults");
            return AppConfig.defaults();
        }

        log.debug("Loading config from {}", CONFIG_FILE);

        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.load(in);

            return toAppConfig(props);
        } catch (IOException | NumberFormatException e) {
            log.warn("Failed to load config, using defaults", e);
            return AppConfig.defaults();
        }
    }

    @Override
    public void save(AppConfig config) {
        log.debug("Saving config to {}", CONFIG_FILE);

        try {
            Files.createDirectories(CONFIG_DIR);

            Properties props = getProperties(config);

            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Code2Prompt Configuration");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + CONFIG_FILE, e);
        }
    }

    private Properties getProperties(AppConfig config) {
        Properties props = new Properties();
        props.setProperty("model.name", config.modelName());
        props.setProperty("model.maxSymbols", String.valueOf(config.maxSymbols()));
        props.setProperty("model.safetyMargin", String.valueOf(config.safetyMargin()));
        props.setProperty("output.path", config.outputPath().toString());
        props.setProperty("log.level", config.logLevel().name());
        props.setProperty("log.error.enabled", String.valueOf(config.errorLogEnabled()));
        props.setProperty("debug.mode", String.valueOf(config.debugMode()));
        return props;
    }

    private AppConfig toAppConfig(Properties props) {
        return new AppConfig(
                props.getProperty("model.name", AppConfig.DEFAULT_MODEL),
                Integer.parseInt(props.getProperty("model.maxSymbols",
                        String.valueOf(AppConfig.DEFAULT_MAX_SYMBOLS))),
                Double.parseDouble(props.getProperty("model.safetyMargin",
                        String.valueOf(AppConfig.DEFAULT_SAFETY_MARGIN))),
                Path.of(props.getProperty("output.path",
                        AppConfig.DEFAULT_OUTPUT_PATH.toString())),
                AppConfig.LogLevel.valueOf(props.getProperty("log.level", "INFO")),
                Boolean.parseBoolean(props.getProperty("log.error.enabled", "true")),
                Boolean.parseBoolean(props.getProperty("debug.mode", "false"))
        );
    }
}