package ru.npepub.filter;

import ru.npepub.config.FilterConfig;

/**
 * Marker interface for filters that can reload their configuration.
 */
public interface ConfigurableFilter {

    /**
     * Reloads internal state from the given configuration.
     *
     * @param config current filter configuration
     */
    void reloadConfig(FilterConfig config);
}