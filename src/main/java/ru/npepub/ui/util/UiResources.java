package ru.npepub.ui.util;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Provides centralized access to application resources.
 */
public final class UiResources {

    private static final String BUNDLE_NAME = "messages";
    private static final String STYLESHEET_PATH = "/css/style.css";

    private UiResources() {
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle(BUNDLE_NAME);
    }

    public static String getStylesheetPath() {
        return Objects.requireNonNull(
                UiResources.class.getResource(STYLESHEET_PATH)
        ).toExternalForm();
    }
}