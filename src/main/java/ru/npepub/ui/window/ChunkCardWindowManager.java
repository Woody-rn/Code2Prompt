package ru.npepub.ui.window;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.ui.controller.ChunkCardWindowController;
import ru.npepub.ui.util.StageBinder;
import ru.npepub.ui.util.UiResources;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Manages the separate window for chunk cards.
 * Cards are accumulated even before the window is shown.
 */
@C2PComponent
public class ChunkCardWindowManager {

    private static final Logger log = LoggerFactory.getLogger(ChunkCardWindowManager.class);

    private final List<Path> pendingFiles = new ArrayList<>();
    private Stage stage;
    private ChunkCardWindowController controller;

    /** Shows the chunk cards window, creating it if needed. */
    public void show(Stage mainStage) {
        if (stage == null) {
            createWindow(mainStage);
            flushPendingFiles();
        }
        stage.show();
        stage.toFront();
    }

    /** Hides the chunk cards window. */
    public void hide() {
        if (stage != null) {
            stage.hide();
        }
    }

    /** Toggles the window visibility. */
    public void toggle(Stage mainStage) {
        if (stage == null || !stage.isShowing()) {
            show(mainStage);
        } else {
            hide();
        }
    }

    /** Adds a result card for the given file. Creates window if needed. */
    public void addChunkCard(Path file) {
        if (controller == null) {
            pendingFiles.add(file);
        } else {
            controller.addChunkCard(file);
        }
    }

    /** Clears all chunk cards, including pending ones. */
    public void clear() {
        pendingFiles.clear();
        if (controller != null) {
            controller.clear();
        }
    }

    private void createWindow(Stage mainStage) {
        try {
            ResourceBundle bundle = UiResources.getBundle();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/chunk-cards-window.fxml"), bundle);
            Parent root = loader.load();
            controller = loader.getController();

            stage = new Stage();
            stage.setTitle(bundle.getString("chunk.results.label"));
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add(UiResources.getStylesheetPath());

            if (mainStage != null) {
                StageBinder.bind(stage, mainStage);
            }

            log.info("Chunk cards window created");
        } catch (IOException e) {
            log.error("Failed to create chunk cards window", e);
            throw new RuntimeException("Failed to create chunk cards window", e);
        }
    }

    private void flushPendingFiles() {
        for (Path file : pendingFiles) {
            controller.addChunkCard(file);
        }
        pendingFiles.clear();
    }
}