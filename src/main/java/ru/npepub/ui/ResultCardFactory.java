package ru.npepub.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

/**
 * Creates chunk card UI components for result files.
 */
@C2PComponent
public class ResultCardFactory {

    private static final Logger log = LoggerFactory.getLogger(ResultCardFactory.class);

    /**
     * Creates a card node for the given file.
     */
    public Node create(Path file) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chunk-card.fxml"), bundle);
            Node card = loader.load();
            loader.<ChunkCardController>getController().setFile(file, Files.readString(file).length());
            return card;
        } catch (IOException e) {
            log.error("Failed to create result card for: {}", file, e);
            throw new RuntimeException("Failed to create result card", e);
        }
    }
}