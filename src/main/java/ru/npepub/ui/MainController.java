package ru.npepub.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.C2PInject;
import ru.npepub.model.AppConfig;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;
import ru.npepub.service.FileScanner;
import ru.npepub.service.OutputWriter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Controller for the main window.
 */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private TextField sourcePathField;
    @FXML private TextField outputPathField;
    @FXML private TextField limitField;
    @FXML private Label modelLabel;
    @FXML private ProgressBar progressBar;
    @FXML private VBox resultsBox;
    @FXML private Label statusLabel;

    @C2PInject private FileScanner fileScanner;
    @C2PInject private FileAggregator fileAggregator;
    @C2PInject private OutputWriter outputWriter;
    @C2PInject private ConfigPort configPort;

    private AppConfig config;

    @FXML
    public void initialize() {
        config = configPort.load();
        limitField.setText(String.valueOf(config.effectiveLimit()));
        modelLabel.setText(config.modelName());
        outputPathField.setText(config.outputPath().toString());
    }

    @FXML
    private void onBrowseSource() {
        File dir = chooseDirectory("Выберите папку с проектом", sourcePathField.getText());
        if (dir != null) {
            sourcePathField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseOutput() {
        File dir = chooseDirectory("Выберите папку для сохранения", outputPathField.getText());
        if (dir != null) {
            outputPathField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void onOpenSettings() {
        // TODO: открыть диалог настроек
        log.debug("Settings button clicked");
    }

    @FXML
    private void onStart() {
        String sourcePath = sourcePathField.getText();
        String outputPath = outputPathField.getText();
        String limitText = limitField.getText();

        if (sourcePath.isEmpty() || outputPath.isEmpty()) {
            setStatus("Укажите папки источника и вывода", true);
            return;
        }

        int limit;
        try {
            limit = Integer.parseInt(limitText);
        } catch (NumberFormatException e) {
            setStatus("Некорректный лимит символов", true);
            return;
        }

        progressBar.setVisible(true);
        resultsBox.getChildren().clear();

        new Thread(() -> {
            try {
                log.info("Starting scan: {}", sourcePath);
                setStatus("Сканирование...");

                List<FileInfo> files = fileScanner.scan(Path.of(sourcePath));
                log.info("Found {} files", files.size());

                setStatus("Разбивка на части...");
                List<Chunk> chunks = fileAggregator.aggregate(files, limit);

                setStatus("Запись файлов...");
                List<Path> writtenFiles = outputWriter.write(chunks, Path.of(outputPath));

                javafx.application.Platform.runLater(() -> {
                    for (Path file : writtenFiles) {
                        addResultCard(file);
                    }
                    progressBar.setVisible(false);
                    setStatus("Готово. Создано " + writtenFiles.size() + " файлов.");
                });

            } catch (Exception e) {
                log.error("Process failed", e);
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    setStatus("Ошибка: " + e.getMessage(), true);
                });
            }
        }).start();
    }

    private void addResultCard(Path file) {
        // TODO: загружать chunk-card.fxml для каждого файла
        Label label = new Label(file.getFileName().toString() + " — " + file.toFile().length() + " байт");
        resultsBox.getChildren().add(label);
    }

    private File chooseDirectory(String title, String initialPath) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File initialDir = new File(initialPath);
        if (initialDir.exists() && initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }
        return chooser.showDialog(sourcePathField.getScene().getWindow());
    }

    private void setStatus(String text) {
        setStatus(text, false);
    }

    private void setStatus(String text, boolean isError) {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: gray;");
        });
    }
}