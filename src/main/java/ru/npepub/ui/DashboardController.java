package ru.npepub.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.ContainerDI;
import ru.npepub.di.api.C2PInject;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.model.AppConfig;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.pipeline.PrepareContextPipeline;
import ru.npepub.ui.log.LogWindowPort;
import ru.npepub.ui.task.TaskRunner;
import ru.npepub.validation.RequestValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Main dashboard controller.
 * Coordinates UI events and delegates work to specialized classes.
 */
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @FXML private TextField sourcePathField;
    @FXML private TextField outputPathField;
    @FXML private TextField limitField;
    @FXML private ProgressBar progressBar;
    @FXML private VBox resultsBox;
    @FXML private Label statusLabel;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private FileTreeController fileTreeController;

    @C2PInject private ConfigPort configPort;
    @C2PInject private ContainerDI container;
    @C2PInject private LogWindowPort logWindowManager;
    @C2PInject private PrepareContextPipeline pipeline;
    @C2PInject private RequestValidator requestValidator;

    private AppConfig config;
    private final TaskRunner taskRunner = new TaskRunner();
    private final ResultCardFactory resultCardFactory = new ResultCardFactory();

    @FXML
    public void initialize() {
        config = configPort.load();
        limitField.setText(String.valueOf(config.effectiveLimit()));
        outputPathField.setText(config.outputPath().toString());
        logWindowManager.setOnClosed(this::disableDebugMode);
        applyLogLevel();

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
                applyLogLevel();
                limitField.setText(String.valueOf(config.effectiveLimit()));
                outputPathField.setText(config.outputPath().toString());
                logWindowManager.toggle(config.debugMode(), getMainStage());
                setStatusBar("Настройки сохранены");
            }
        } catch (IOException e) {
            log.error("Failed to open settings", e);
        }
    }

    @FXML
    private void onStart() {
        PrepareRequest request = prepareRequest();

        requestValidator.validate(request).ifPresentOrElse(
                error -> setStatusBar(error.description(), true),
                () -> startScanTask(request)
        );
    }

    @FXML
    private void onStop() {
        taskRunner.cancel();
        setStatusBar("Отмена...");
    }

    private void startScanTask(PrepareRequest request) {
        toggleButtons(true);
        progressBar.setVisible(true);
        resultsBox.getChildren().clear();
        fileTreeController.clear();

        taskRunner.run(
                (onProgress, cancelled) -> {
                    List<FileInfo> files = pipeline.scan(request);
                    if (cancelled.get()) return List.of();
                    Platform.runLater(() -> fileTreeController.populate(files));

                    onProgress.accept("Разбивка на части...");
                    List<Chunk> chunks = pipeline.aggregate(request, files);
                    if (cancelled.get()) return List.of();

                    onProgress.accept("Запись файлов...");
                    return pipeline.write(request, chunks);
                },
                this::setStatusBar,
                file -> resultsBox.getChildren().add(resultCardFactory.create(file)),
                () -> {
                    progressBar.setVisible(false);
                    toggleButtons(false);
                }
        );
    }

    private void toggleButtons(boolean running) {
        startButton.setVisible(!running);
        stopButton.setVisible(running);
    }

    private File chooseDirectory(String title, String initialPath) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File dir = new File(initialPath);
        if (dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);
        return chooser.showDialog(sourcePathField.getScene().getWindow());
    }

    private void setStatusBar(String text) {
        setStatusBar(text, false);
    }

    private void setStatusBar(String text, boolean isError) {
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
            setStatusBar("Не удалось открыть папку с логами", true);
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

    private void applyLogLevel() {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.toLevel(config.logLevel().name()));
    }

    private PrepareRequest prepareRequest() {
        return new PrepareRequest(
                sourcePathField.getText(),
                outputPathField.getText(),
                limitField.getText()
        );
    }
}