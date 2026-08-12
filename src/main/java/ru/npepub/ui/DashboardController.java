package ru.npepub.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.AppConfig;
import ru.npepub.config.ConfigPort;
import ru.npepub.config.PromptConfig;
import ru.npepub.di.ContainerDI;
import ru.npepub.di.api.C2PInject;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.dto.ValidationError;
import ru.npepub.model.ProjectInfo;
import ru.npepub.ui.coordinator.ContextServerLauncher;
import ru.npepub.ui.coordinator.ProjectHistoryStore;
import ru.npepub.ui.coordinator.ScanPipelineRunner;
import ru.npepub.ui.coordinator.TaskTemplateManager;
import ru.npepub.ui.log.LogWindowPort;
import ru.npepub.ui.util.ProjectPathResolver;
import ru.npepub.ui.validation.RequestValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Main dashboard controller.
 * Coordinates UI events and delegates work to specialized classes.
 */
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @FXML
    private ComboBox<String> sourcePathField;
    @FXML
    private TextField outputPathField;
    @FXML
    private TextField limitField;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private VBox resultsBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button serverButton;
    @FXML
    private FileTreeController fileTreeController;
    @FXML
    private Label serverIndicator;
    @FXML
    private TextArea promptField;
    @FXML
    private ComboBox<String> taskCombo;

    @C2PInject
    private ConfigPort configPort;
    @C2PInject
    private ContainerDI container;
    @C2PInject
    private LogWindowPort logWindowManager;
    @C2PInject
    private RequestValidator requestValidator;
    @C2PInject
    private ScanPipelineRunner pipelineRunner;
    @C2PInject
    private ContextServerLauncher serverLauncher;
    @C2PInject
    private ProjectHistoryStore projectHistory;
    @C2PInject
    private ResultCardFactory resultCardFactory;
    @C2PInject
    private TaskTemplateManager templateManager;

    private AppConfig config;
    private ProjectInfo projectInfo;
    private PrepareRequest lastRequest;
    private ResourceBundle messages;

    @FXML
    public void initialize() {
        messages = ResourceBundle.getBundle("messages");
        config = configPort.load();
        limitField.setText(String.valueOf(config.effectiveLimit()));
        outputPathField.setText(config.paths().outputPath().toString());
        logWindowManager.setOnClosed(this::disableDebugMode);
        applyLogLevel();

        sourcePathField.getItems().setAll(projectHistory.getAll());
        updateServerUI(false);

        setupDragAndDrop();
        fileTreeController.setStatusConsumer(this::setStatusBar);

        promptField.setText(config.prompt().systemPrompt());
        buildTaskCombo();

        if (config.debugMode()) {
            Platform.runLater(() -> logWindowManager.show(getMainStage()));
        }

        Platform.runLater(() -> {
            Stage stage = getMainStage();
            if (stage != null) stage.setOnCloseRequest(event -> serverLauncher.stop());
        });
    }

    private void buildTaskCombo() {
        taskCombo.getItems().clear();
        taskCombo.getItems().addAll(templateManager.getBuiltInTaskNames(messages));

        Map<String, String> custom = templateManager.getCustomTemplates();
        if (!custom.isEmpty()) {
            taskCombo.getItems().add("──────────");
            taskCombo.getItems().addAll(custom.keySet());
        }

        taskCombo.setOnAction(e -> {
            String selected = taskCombo.getValue();
            if (selected == null || selected.equals(messages.getString("task.custom"))) {
                promptField.clear();
                return;
            }
            if (selected.equals("──────────")) return;

            String prompt = templateManager.getPromptForTask(selected, messages);
            if (prompt != null) {
                promptField.setText(prompt);
            }
        });

        promptField.textProperty().addListener((obs, old, val) -> {
            Platform.runLater(() -> {
                String selected = taskCombo.getValue();
                String expectedPrompt = templateManager.getPromptForTask(selected, messages);
                if (expectedPrompt == null || !expectedPrompt.equals(val)) {
                    taskCombo.setValue(messages.getString("task.custom"));
                }
            });
        });
    }

    @FXML
    private void onSaveTemplate() {
        String prompt = promptField.getText();
        if (prompt == null || prompt.isBlank()) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(messages.getString("template.save.title"));
        dialog.setHeaderText(null);
        dialog.setContentText(messages.getString("template.save.prompt"));

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            templateManager.saveOrUpdateTemplate(name, prompt);
            buildTaskCombo();
            taskCombo.setValue(name);
        });
    }

    @FXML
    private void onEditTemplate() {
        String selected = taskCombo.getValue();
        if (selected == null || templateManager.isBuiltIn(selected, messages)) return;
        String text = templateManager.getCustomTemplates().get(selected);
        if (text != null) {
            promptField.setText(text);
        }
    }

    @FXML
    private void onDeleteTemplate() {
        String selected = taskCombo.getValue();
        if (selected == null || templateManager.isBuiltIn(selected, messages)) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(messages.getString("template.delete.confirm"));
        confirm.setHeaderText(null);
        confirm.setContentText(MessageFormat.format(messages.getString("template.delete.confirm"), selected));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                templateManager.deleteTemplate(selected);
                buildTaskCombo();
                taskCombo.setValue(messages.getString("task.custom"));
                promptField.clear();
            }
        });
    }

    private void setupDragAndDrop() {
        sourcePathField.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        sourcePathField.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            if (!files.isEmpty() && files.getFirst().isDirectory()) {
                sourcePathField.getEditor().setText(files.getFirst().getAbsolutePath());
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    @FXML
    private void onBrowseSource() {
        File dir = chooseDirectory(messages.getString("browse.source.title"), sourcePathField.getEditor().getText());
        if (dir != null) sourcePathField.getEditor().setText(dir.getAbsolutePath());
    }

    @FXML
    private void onBrowseOutput() {
        File dir = chooseDirectory(messages.getString("browse.output.title"), outputPathField.getText());
        if (dir != null) outputPathField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onOpenSourceFolder() {
        openFolder(sourcePathField.getEditor().getText());
    }

    @FXML
    private void onOpenOutputFolder() {
        openFolder(outputPathField.getText());
    }

    @FXML
    private void onStart() {
        String sourcePath = sourcePathField.getEditor().getText();
        sourcePathField.setValue(sourcePath);
        projectInfo = ProjectInfo.from(sourcePath);
        updateOutputPathWithProjectName();

        savePromptToConfig();

        PrepareRequest request = prepareRequest();
        requestValidator.validate(request).ifPresentOrElse(
                this::setStatusBar,
                () -> {
                    projectHistory.add(sourcePath);
                    sourcePathField.getItems().setAll(projectHistory.getAll());
                    startScanTask(request);
                }
        );
    }

    @FXML
    private void onStop() {
        pipelineRunner.cancel();
        setStatusBar(messages.getString("status.cancelling"));
    }

    private void startScanTask(PrepareRequest request) {
        stopServerIfRunning();
        toggleButtons(true);
        progressBar.setVisible(true);
        resultsBox.getChildren().clear();
        fileTreeController.clear();

        pipelineRunner.run(
                request,
                this::setStatusBar,
                this::addResultCard,
                () -> {
                    progressBar.setVisible(false);
                    toggleButtons(false);
                    lastRequest = request;
                },
                files -> fileTreeController.populate(files, projectInfo.name(), Path.of(request.sourcePath())),
                this::setStatusBar
        );
    }

    private void addResultCard(Path file) {
        resultsBox.getChildren().add(resultCardFactory.create(file));
    }

    @FXML
    private void onToggleServer() {
        if (serverLauncher.isRunning()) {
            serverLauncher.stop();
            updateServerUI(false);
            setStatusBar(messages.getString("status.server.stopped"));
        } else if (lastRequest != null && projectInfo != null) {
            try {
                serverLauncher.start(Path.of(lastRequest.outputPath()), projectInfo);
                updateServerUI(true);
                setStatusBar(messages.getString("status.server.started"));
            } catch (Exception e) {
                log.error("Failed to start HTTPS server", e);
                setStatusBar(messages.getString("status.server.error") + " " + e.getMessage(), true);
            }
        } else {
            setStatusBar(messages.getString("status.scan.first"), true);
        }
    }

    private void stopServerIfRunning() {
        if (serverLauncher.isRunning()) {
            serverLauncher.stop();
            updateServerUI(false);
        }
    }

    private void updateServerUI(boolean running) {
        if (running) {
            serverButton.setText(messages.getString("server.stop.button"));
            serverIndicator.getStyleClass().setAll("server-on");
        } else {
            serverButton.setText(messages.getString("server.start.button"));
            serverIndicator.getStyleClass().setAll("server-off");
        }
    }

    @FXML
    private void onOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"), messages);
            loader.setControllerFactory(container::createController);
            DialogPane pane = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(messages.getString("settings.title"));
            dialog.setDialogPane(pane);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                AppConfig updated = ((SettingsController) loader.getController()).getUpdatedConfig();
                configPort.save(updated);
                config = updated;
                applyLogLevel();
                limitField.setText(String.valueOf(config.effectiveLimit()));
                outputPathField.setText(config.paths().outputPath().toString());
                logWindowManager.toggle(config.debugMode(), getMainStage());
                sourcePathField.getItems().setAll(projectHistory.getAll());
                buildTaskCombo();
                setStatusBar(messages.getString("status.saved"));
            }
        } catch (IOException e) {
            log.error("Failed to open settings", e);
        }
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

    private void openFolder(String path) {
        try {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) java.awt.Desktop.getDesktop().open(dir);
            else setStatusBar(messages.getString("status.folder.notfound"), true);
        } catch (IOException e) {
            log.error("Failed to open folder: {}", path, e);
            setStatusBar(messages.getString("status.folder.open.error"), true);
        }
    }

    private void savePromptToConfig() {
        String prompt = promptField.getText();
        if (prompt != null && !prompt.equals(config.prompt().systemPrompt())) {
            config = new AppConfig(
                    config.aiModel(), config.paths(), config.filter(), config.log(),
                    new PromptConfig(prompt, config.prompt().partPrefixTemplate(),
                            config.prompt().finalPartTemplate(), config.prompt().fileSeparator(),
                            config.prompt().customTemplates()),
                    config.debugMode()
            );
            configPort.save(config);
        }
    }

    private void setStatusBar(String text) {
        setStatusBar(text, false);
    }

    private void setStatusBar(ValidationError error) {
        setStatusBar(error.description(), true);
    }

    private void setStatusBar(String text, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: gray;");
        });
    }

    private Stage getMainStage() {
        return (Stage) sourcePathField.getScene().getWindow();
    }

    private void disableDebugMode() {
        config = new AppConfig(
                config.aiModel(), config.paths(), config.filter(), config.log(),
                config.prompt(), false
        );
        configPort.save(config);
    }

    private void applyLogLevel() {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.toLevel(config.log().level().name()));
    }

    private PrepareRequest prepareRequest() {
        return new PrepareRequest(
                sourcePathField.getEditor().getText(),
                outputPathField.getText(),
                limitField.getText()
        );
    }

    private void updateOutputPathWithProjectName() {
        outputPathField.setText(ProjectPathResolver.resolveOutputPath(
                projectInfo, config.paths().outputPath().toString()));
    }
}