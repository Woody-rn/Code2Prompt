package ru.npepub.config;

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