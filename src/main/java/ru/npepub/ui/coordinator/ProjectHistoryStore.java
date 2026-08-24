package ru.npepub.ui.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Stores and manages recent project paths in a dedicated history file.
 */
@C2PComponent
public class ProjectHistoryStore {

    private static final Logger log = LoggerFactory.getLogger(ProjectHistoryStore.class);

    private static final Path HISTORY_DIR = Path.of(
            System.getProperty("user.home"), ".code2prompt"
    );
    private static final Path HISTORY_FILE = HISTORY_DIR.resolve("history.properties");

    private static final int MAX_RECENT = 10;

    /** Adds a path to the top of recent projects. Trims to max size. */
    public void add(String path) {
        if (path == null || path.isBlank()) return;

        List<String> projects = new ArrayList<>(getAll());
        projects.remove(path);
        projects.addFirst(path);

        if (projects.size() > MAX_RECENT) {
            projects = projects.subList(0, MAX_RECENT);
        }

        save(projects);
        log.debug("Added recent project: {}", path);
    }

    /** Returns an immutable copy of all recent project paths. */
    public List<String> getAll() {
        if (!Files.exists(HISTORY_FILE)) {
            return List.of();
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(HISTORY_FILE)) {
            props.load(in);
        } catch (IOException e) {
            log.warn("Failed to read history file", e);
            return List.of();
        }

        String value = props.getProperty("recent.projects", "");
        if (value.isEmpty()) return List.of();

        List<String> projects = new ArrayList<>();
        for (String p : value.split(";")) {
            if (!p.isBlank()) projects.add(p);
        }
        return List.copyOf(projects);
    }

    private void save(List<String> projects) {
        try {
            Files.createDirectories(HISTORY_DIR);
            Properties props = new Properties();
            props.setProperty("recent.projects", String.join(";", projects));
            try (OutputStream out = Files.newOutputStream(HISTORY_FILE)) {
                props.store(out, "Code2Prompt Recent Projects");
            }
        } catch (IOException e) {
            log.warn("Failed to save history file", e);
        }
    }
}