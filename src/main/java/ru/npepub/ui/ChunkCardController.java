package ru.npepub.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for a single chunk card with copy button.
 */
public class ChunkCardController {

    private static final Logger log = LoggerFactory.getLogger(ChunkCardController.class);

    @FXML
    private Label fileNameLabel;
    @FXML
    private Label sizeLabel;
    @FXML
    @SuppressWarnings("unused")
    private Button copyButton;
    @FXML
    @SuppressWarnings("unused")
    private Button openButton;
    @FXML
    private Label copiedLabel;

    private Path filePath;

    /**
     * Sets the file info for this card.
     */
    public void setFile(Path filePath, long size) {
        this.filePath = filePath;
        fileNameLabel.setText(filePath.getFileName().toString());
        sizeLabel.setText(formatSize(size));
    }

    @FXML
    private void onCopy() {
        try {
            String content = Files.readString(filePath);
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(content), null);

            copiedLabel.setVisible(true);
        } catch (Exception e) {
            log.error("Failed to copy", e);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " б";
        if (bytes < 1024 * 1024) return String.format("%.1f КБ", bytes / 1024.0);
        return String.format("%.1f МБ", bytes / (1024.0 * 1024));
    }

    @FXML
    private void onOpen() {
        try {
            java.awt.Desktop.getDesktop().open(filePath.toFile());
        } catch (Exception e) {
            log.error("Failed to open file", e);
        }
    }
}