package ru.npepub.config;

import ru.npepub.model.AppConfig;

/**
 * Port for loading and persisting application configuration.
 */
public interface ConfigPort {

    /**
     * @return loaded configuration or defaults if unavailable
     */
    AppConfig load();

    /**
     * @param config configuration to persist
     */
    void save(AppConfig config);
}