package ru.npepub.ui.util;

import javafx.stage.Stage;

/**
 * Binds a secondary window to the main window.
 * The secondary window follows the main window's position and size.
 */
public final class StageBinder {

    private StageBinder() {
    }

    /**
     * Positions the secondary stage to the right of the main stage
     * and keeps it synchronized when the main stage moves or resizes.
     *
     * @param secondary  the window to bind
     * @param main       the main window to follow
     */
    public static void bind(Stage secondary, Stage main) {
        secondary.setX(main.getX() + main.getWidth());
        secondary.setY(main.getY());
        secondary.setHeight(main.getHeight());

        main.xProperty().addListener((obs, oldX, newX) -> {
            if (secondary.isShowing()) {
                secondary.setX(newX.doubleValue() + main.getWidth());
            }
        });

        main.yProperty().addListener((obs, oldY, newY) -> {
            if (secondary.isShowing()) {
                secondary.setY(newY.doubleValue());
            }
        });

        main.heightProperty().addListener((obs, oldH, newH) -> {
            if (secondary.isShowing()) {
                secondary.setHeight(newH.doubleValue());
            }
        });
    }
}