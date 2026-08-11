package ru.npepub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
            Properties props = toProperties(config);
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Code2Prompt Configuration");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + CONFIG_FILE, e);
        }
    }

    private Properties toProperties(AppConfig c) {
        Properties p = new Properties();
        p.setProperty("ai.model.name", c.aiModel().name());
        p.setProperty("ai.model.maxSymbols", String.valueOf(c.aiModel().maxSymbols()));
        p.setProperty("ai.model.safetyMargin", String.valueOf(c.aiModel().safetyMargin()));
        p.setProperty("paths.output", c.paths().outputPath().toString());
        p.setProperty("paths.recent", String.join(";", c.paths().recentProjects()));
        p.setProperty("paths.recent.count", String.valueOf(c.paths().recentProjectsCount()));
        p.setProperty("filter.excluded.dirs", String.join(";", c.filter().excludedDirs()));
        p.setProperty("filter.excluded.files", String.join(";", c.filter().excludedFileNames()));
        p.setProperty("filter.patterns", String.join(";", c.filter().patterns()));
        p.setProperty("log.level", c.log().level().name());
        p.setProperty("log.error.enabled", String.valueOf(c.log().errorEnabled()));
        p.setProperty("prompt.system", c.prompt().systemPrompt());
        p.setProperty("prompt.partPrefix", c.prompt().partPrefixTemplate());
        p.setProperty("prompt.finalPart", c.prompt().finalPartTemplate());
        p.setProperty("prompt.fileSeparator", c.prompt().fileSeparator());
        p.setProperty("debug.mode", String.valueOf(c.debugMode()));
        return p;
    }

    private AppConfig toAppConfig(Properties p) {
        return new AppConfig(
                loadAiModel(p),
                loadPaths(p),
                loadFilter(p),
                loadLog(p),
                loadPrompt(p),
                Boolean.parseBoolean(p.getProperty("debug.mode", "false"))
        );
    }

    private AiModelConfig loadAiModel(Properties p) {
        return new AiModelConfig(
                p.getProperty("ai.model.name", AiModelConfig.defaults().name()),
                Integer.parseInt(p.getProperty("ai.model.maxSymbols",
                        String.valueOf(AiModelConfig.defaults().maxSymbols()))),
                Double.parseDouble(p.getProperty("ai.model.safetyMargin",
                        String.valueOf(AiModelConfig.defaults().safetyMargin())))
        );
    }

    private PathConfig loadPaths(Properties p) {
        List<String> recent = Arrays.stream(p.getProperty("paths.recent", "").split(";"))
                .filter(s -> !s.isEmpty()).toList();
        int recentCount = Integer.parseInt(p.getProperty("paths.recent.count", "10"));
        return new PathConfig(
                Path.of(p.getProperty("paths.output", PathConfig.defaults().outputPath().toString())),
                recent, recentCount
        );
    }

    private FilterConfig loadFilter(Properties p) {
        List<String> dirs = loadList(p, "filter.excluded.dirs", FilterConfig.defaults().excludedDirs());
        List<String> files = loadList(p, "filter.excluded.files", FilterConfig.defaults().excludedFileNames());
        List<String> patterns = loadList(p, "filter.patterns", FilterConfig.defaults().patterns());
        return new FilterConfig(dirs, files, patterns);
    }

    private LogConfig loadLog(Properties p) {
        return new LogConfig(
                LogConfig.LogLevel.valueOf(p.getProperty("log.level", LogConfig.defaults().level().name())),
                Boolean.parseBoolean(p.getProperty("log.error.enabled",
                        String.valueOf(LogConfig.defaults().errorEnabled())))
        );
    }

    private PromptConfig loadPrompt(Properties p) {
        return new PromptConfig(
                p.getProperty("prompt.system", PromptConfig.defaults().systemPrompt()),
                p.getProperty("prompt.partPrefix", PromptConfig.defaults().partPrefixTemplate()),
                p.getProperty("prompt.finalPart", PromptConfig.defaults().finalPartTemplate()),
                p.getProperty("prompt.fileSeparator", PromptConfig.defaults().fileSeparator())
        );
    }

    private List<String> loadList(Properties p, String key, List<String> defaults) {
        String value = p.getProperty(key, "");
        if (value.isEmpty()) return defaults;
        return Arrays.stream(value.split(";")).filter(s -> !s.isEmpty()).toList();
    }
}