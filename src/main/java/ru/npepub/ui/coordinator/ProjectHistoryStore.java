package ru.npepub.ui.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.AppConfig;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores and manages recent project paths in application config.
 */
@C2PComponent
public class ProjectHistoryStore {

    private static final Logger log = LoggerFactory.getLogger(ProjectHistoryStore.class);

    @C2PInject
    private ConfigPort configPort;

    /**
     * Adds a path to the top of recent projects list.
     * Trims the list to the configured maximum size.
     */
    public void add(String path) {
        if (path == null || path.isBlank()) return;

        AppConfig config = configPort.load();
        List<String> projects = new ArrayList<>(config.recentProjects());
        projects.remove(path);
        projects.addFirst(path);

        int max = config.recentProjectsCount();
        if (projects.size() > max) {
            projects = projects.subList(0, max);
        }

        configPort.save(withRecentProjects(config, projects));
        log.debug("Added recent project: {}", path);
    }

    /**
     * Returns all recent project paths from config.
     */
    public List<String> getAll() {
        return List.copyOf(configPort.load().recentProjects());
    }

    private AppConfig withRecentProjects(AppConfig config, List<String> projects) {
        return new AppConfig(
                config.modelName(), config.maxSymbols(), config.safetyMargin(),
                config.outputPath(), config.logLevel(), config.errorLogEnabled(),
                config.debugMode(), projects, config.recentProjectsCount(),
                config.excludedDirs(), config.excludedFileNames()
        );
    }
}