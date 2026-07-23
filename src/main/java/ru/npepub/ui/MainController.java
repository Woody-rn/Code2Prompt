package ru.npepub.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.api.C2PInject;
import ru.npepub.di.ContainerDI;
import ru.npepub.model.AppConfig;
import ru.npepub.service.FileAggregator;
import ru.npepub.service.FileScanner;
import ru.npepub.service.OutputWriter;
import ru.npepub.ui.log.LogWindowPort;
import ru.npepub.ui.task.ScanTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Controller for the main window.
 * Coordinates UI events and delegates work to specialized classes.
 */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private TextField sourcePathField;
    @FXML private TextField outputPathField;
    @FXML private TextField limitField;
    @FXML private ProgressBar progressBar;
    @FXML private VBox resultsBox;
    @FXML private Label statusLabel;

    @C2PInject private FileScanner fileScanner;
    @C2PInject private FileAggregator fileAggregator;
    @C2PInject private OutputWriter outputWriter;
    @C2PInject private ConfigPort configPort;
    @C2PInject private ContainerDI container;
    @C2PInject private LogWindowPort logWindowManager;

    private AppConfig config;

    @FXML
    public void initialize() {
        config = configPort.load();
        limitField.setText(String.valueOf(config.effectiveLimit()));
        outputPathField.setText(config.outputPath().toString());

        logWindowManager.setOnClosed(this::disableDebugMode);


        if (config.debugMode()) {
            Platform.runLater(() -> logWindowManager.show(getMainStage()));
        }
    }

    @FXML
    private void onBrowseSource() {
        File dir = chooseDirectory("Выберите папку с проектом", sourcePathField.getText());
        if (dir != null) sourcePathField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onBrowseOutput() {
        File dir = chooseDirectory("Выберите папку для сохранения", outputPathField.getText());
        if (dir != null) outputPathField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            loader.setControllerFactory(container::createController);
            DialogPane pane = loader.load();
            SettingsController ctrl = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Настройки");
            dialog.setDialogPane(pane);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                AppConfig updated = ctrl.getUpdatedConfig();
                configPort.save(updated);
                config = updated;
                limitField.setText(String.valueOf(config.effectiveLimit()));
                outputPathField.setText(config.outputPath().toString());
                logWindowManager.toggle(config.debugMode(), getMainStage());
                setStatus("Настройки сохранены");
            }
        } catch (IOException e) {
            log.error("Failed to open settings", e);
        }
    }

    @FXML
    private void onStart() {
        String source = sourcePathField.getText();
        String output = outputPathField.getText();
        String limitText = limitField.getText();

        Optional<String> error = validateInputs(source, output, limitText);
        if (error.isPresent()) {
            setStatus(error.get(), true);
            return;
        }

        int limit = Integer.parseInt(limitText);
        startScanTask(source, output, limit);
    }

    private void startScanTask(String source, String output, int limit) {
        progressBar.setVisible(true);
        resultsBox.getChildren().clear();

        new ScanTask(fileScanner, fileAggregator, outputWriter, source, output, limit,
                this::setStatus,
                this::addResultCard,
                () -> progressBar.setVisible(false)
        ).start();
    }

    private Optional<String> validateInputs(String source, String output, String limitText) {
        if (source == null || source.isBlank()) {
            return Optional.of("Укажите папку-источник");
        }
        if (output == null || output.isBlank()) {
            return Optional.of("Укажите папку вывода");
        }
        try {
            int limit = Integer.parseInt(limitText);
            if (limit <= 0) {
                return Optional.of("Лимит должен быть положительным числом");
            }
        } catch (NumberFormatException e) {
            return Optional.of("Некорректный лимит символов");
        }
        return Optional.empty();
    }

    private void addResultCard(Path file) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chunk-card.fxml"));
            Node card = loader.load();

            ChunkCardController controller = loader.getController();
            String content = Files.readString(file);
            controller.setFile(file, content.length());

            resultsBox.getChildren().add(card);
        } catch (IOException e) {
            log.error("Failed to load chunk card", e);
        }
    }

    private File chooseDirectory(String title, String initialPath) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File dir = new File(initialPath);
        if (dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);
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

    private Stage getMainStage() {
        return (Stage) sourcePathField.getScene().getWindow();
    }

    private void disableDebugMode() {
        config = new AppConfig(
                config.modelName(), config.maxSymbols(), config.safetyMargin(),
                config.outputPath(), config.logLevel(), config.errorLogEnabled(), false
        );
        configPort.save(config);
    }
}