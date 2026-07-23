package ru.npepub.ui.log;

import javafx.stage.Stage;

/**
 * Port for managing the debug log window.
 */
public interface LogWindowPort {

    /**
     * Shows the log window attached to the given main stage.
     */
    void show(Stage mainStage);

    /**
     * Hides and disposes the log window.
     */
    void hide();

    /**
     * Shows or hides based on the flag.
     */
    void toggle(boolean enabled, Stage mainStage);

    /**
     * @return true if the log window is currently visible
     */
    boolean isShowing();
}