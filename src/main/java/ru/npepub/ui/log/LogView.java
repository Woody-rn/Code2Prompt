package ru.npepub.ui.log;

/**
 * Port for displaying log messages in the UI.
 */
public interface LogView {

    /**
     * Start capturing logs and displaying them in the view.
     */
    void attach();

    /**
     * Stop capturing logs.
     */
    void detach();
}