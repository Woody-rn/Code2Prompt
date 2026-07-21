package ru.npepub.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for a single chunk card with copy button.
 */
public class ChunkCardController {

    @FXML private Label fileNameLabel;
    @FXML private Label sizeLabel;
    @FXML private Button copyButton;
    @FXML private Label copiedLabel;

    private Path filePath;
    private boolean copied = false;

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

            copied = true;
            copyButton.setVisible(false);
            copiedLabel.setVisible(true);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * @return true if content was already copied
     */
    public boolean isCopied() {
        return copied;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " б";
        if (bytes < 1024 * 1024) return String.format("%.1f КБ", bytes / 1024.0);
        return String.format("%.1f МБ", bytes / (1024.0 * 1024));
    }
}