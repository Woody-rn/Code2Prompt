package ru.npepub.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.npepub.ui.component.ResultCardFactory;

import java.nio.file.Path;

/**
 * Controller for the separate chunk cards window.
 */
public class ChunkCardWindowController {

    @FXML
    private VBox chunkCardBox;

    private final ResultCardFactory resultCardFactory = new ResultCardFactory();

    /** Adds a result card for the given file. */
    public void addChunkCard(Path file) {
        chunkCardBox.getChildren().add(resultCardFactory.create(file));
    }

    /** Clears all chunk cards. */
    public void clear() {
        chunkCardBox.getChildren().clear();
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) chunkCardBox.getScene().getWindow();
        stage.hide();
    }
}