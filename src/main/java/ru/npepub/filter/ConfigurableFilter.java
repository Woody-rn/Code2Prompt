package ru.npepub.filter;

/**
 * Marker interface for filters that can reload their configuration.
 */
public interface ConfigurableFilter {

    /**
     * Reloads internal state from current configuration.
     */
    void reloadConfig();
}