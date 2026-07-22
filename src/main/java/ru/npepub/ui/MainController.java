package ru.npepub.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.C2PInject;
import ru.npepub.di.ContainerDI;
import ru.npepub.model.AppConfig;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;
import ru.npepub.service.FileScanner;
import ru.npepub.service.OutputWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the main window.
 */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private TextField sourcePathField;
    @FXML private TextField outputPathField;
    @FXML private TextField limitField;
    @FXML private ProgressBar progressBar;
    @FXML private VBox resultsBox;
    @FXML private Label statusLabel;

    @SuppressWarnings("unused")
    @C2PInject private FileScanner fileScanner;
    @SuppressWarnings("unused")
    @C2PInject private FileAggregator fileAggregator;
    @SuppressWarnings("unused")
    @C2PInject private OutputWriter outputWriter;
    @SuppressWarnings("unused")
    @C2PInject private ConfigPort configPort;
    @SuppressWarnings("unused")
    @C2PInject private ContainerDI container;

    private AppConfig config;
    private Stage logStage;
    private TextArea logTextArea;

    @FXML
    public void initialize() {
        config = configPort.load();
        limitField.setText(String.valueOf(config.effectiveLimit()));
        outputPathField.setText(config.outputPath().toString());

        if (config.debugMode()) {
            showLogWindow();
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            loader.setControllerFactory(container::createAndInject);
            DialogPane settingsPane = loader.load();

            SettingsController settingsController = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Настройки");
            dialog.setDialogPane(settingsPane);

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                AppConfig updatedConfig = settingsController.getUpdatedConfig();
                configPort.save(updatedConfig);
                config = updatedConfig;
                limitField.setText(String.valueOf(config.effectiveLimit()));
                outputPathField.setText(config.outputPath().toString());
                toggleDebugMode(config.debugMode());
                setStatus("Настройки сохранены");
            }
        } catch (IOException e) {
            log.error("Failed to open settings", e);
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chunk-card.fxml"));
            Node card = loader.load();

            ChunkCardController controller = loader.getController();
            controller.setFile(file, Files.size(file));

            resultsBox.getChildren().add(card);
        } catch (IOException e) {
            log.error("Failed to load chunk card", e);
        }
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

    @FXML
    private void onOpenLogs() {
        try {
            Path logDir = Path.of(System.getProperty("user.home"), ".code2prompt", "logs");
            Files.createDirectories(logDir);
            java.awt.Desktop.getDesktop().open(logDir.toFile());
        } catch (IOException e) {
            log.error("Failed to open logs folder", e);
            setStatus("Не удалось открыть папку с логами", true);
        }
    }

    private void toggleDebugMode(boolean enabled) {
        if (enabled) {
            showLogWindow();
        } else {
            hideLogWindow();
        }
    }

    private void showLogWindow() {
        if (logStage != null) return;

        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11;");

        logStage = new Stage();
        logStage.setTitle("Логи Code2Prompt");
        logStage.setScene(new Scene(new StackPane(logTextArea), 600, 450));

        Stage mainStage = (Stage) sourcePathField.getScene().getWindow();
        logStage.setX(mainStage.getX() + mainStage.getWidth());
        logStage.setY(mainStage.getY());
        logStage.setHeight(mainStage.getHeight());

        mainStage.xProperty().addListener((obs, old, val) ->
                logStage.setX(val.doubleValue() + mainStage.getWidth()));
        mainStage.yProperty().addListener((obs, old, val) ->
                logStage.setY(val.doubleValue()));
        mainStage.heightProperty().addListener((obs, old, val) ->
                logStage.setHeight(val.doubleValue()));

        logStage.show();
        LogAppender.install(logTextArea);
    }

    private void hideLogWindow() {
        if (logStage != null) {
            LogAppender.uninstall();
            logStage.close();
            logStage = null;
            logTextArea = null;
        }
    }
}