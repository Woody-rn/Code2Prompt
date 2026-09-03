package ru.npepub.ui.controller;

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
import ru.npepub.ai.PromptAssistant;
import ru.npepub.config.*;
import ru.npepub.di.ContainerDI;
import ru.npepub.di.api.C2PInject;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.dto.ValidationError;
import ru.npepub.model.ProjectInfo;
import ru.npepub.ui.service.ContextServerLauncher;
import ru.npepub.ui.service.HelpService;
import ru.npepub.ui.service.ProjectHistoryStore;
import ru.npepub.ui.service.TaskTemplateManager;
import ru.npepub.ui.service.ScanPipelineRunner;
import ru.npepub.ui.util.UiResources;
import ru.npepub.ui.window.ChunkCardWindowManager;
import ru.npepub.update.VersionChecker;
import ru.npepub.ui.log.LogWindowPort;
import ru.npepub.ui.util.ProjectPathResolver;
import ru.npepub.ui.validation.RequestValidator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main dashboard controller.
 * Coordinates UI events and delegates work to specialized classes.
 */
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @FXML private ComboBox<String> sourcePathField;
    @FXML private TextField outputPathField;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button startStopButton;
    @FXML private Button serverButton;
    @FXML private FileTreeController fileTreeController;
    @FXML private TextArea promptField;
    @FXML private ComboBox<String> taskCombo;
    @FXML private VBox promptSection;
    @FXML private Label aiStatusIndicator;
    @FXML private Label aiModelLabel;

    @C2PInject private ConfigPort configPort;
    @C2PInject private ContainerDI container;
    @C2PInject private LogWindowPort logWindowManager;
    @C2PInject private RequestValidator requestValidator;
    @C2PInject private ScanPipelineRunner pipelineRunner;
    @C2PInject private ContextServerLauncher serverLauncher;
    @C2PInject private ProjectHistoryStore projectHistory;
    @C2PInject private TaskTemplateManager templateManager;
    @C2PInject private HelpService helpService;
    @C2PInject private VersionChecker versionChecker;
    @C2PInject private PromptAssistant promptAssistant;
    @C2PInject private ChunkCardWindowManager chunkCardWindow;

    private AppConfig config;
    private ProjectInfo projectInfo;
    private PrepareRequest lastRequest;
    private ResourceBundle messages;
    private boolean updatingPrompt = false;
    private boolean running = false;
    private Timer saveTimer;
    private ScheduledExecutorService aiScheduler;
    private boolean aiPolling = false;

    @FXML
    public void initialize() {
        messages = UiResources.getBundle();
        config = configPort.load();
        outputPathField.setText(config.paths().outputPath().toString());
        logWindowManager.setOnClosed(this::disableDebugMode);
        applyLogLevel();

        sourcePathField.getItems().setAll(projectHistory.getAll());
        updateServerUI(false);

        setupDragAndDrop();
        fileTreeController.setStatusConsumer(this::setStatusBar);

        updatingPrompt = true;
        promptField.setText(config.prompt().systemPrompt());
        updatingPrompt = false;
        buildTaskCombo();

        checkAiAvailability();

        if (config.debugMode()) {
            Platform.runLater(() -> logWindowManager.show(getMainStage()));
        }

        Platform.runLater(() -> {
            Stage stage = getMainStage();
            if (stage != null) {
                stage.setOnCloseRequest(event -> {
                    stopAiPolling();
                    serverLauncher.stop();
                });
            }
            checkForUpdates();
        });
    }

    private void checkAiAvailability() {
        new Thread(() -> {
            boolean available = promptAssistant.isAvailable();
            Platform.runLater(() -> updateAiIndicator(available));
        }).start();
    }

    private void checkAiAvailabilityNow() {
        checkAiAvailability();
    }

    private void updateAiIndicator(boolean available) {
        if (available) {
            aiStatusIndicator.getStyleClass().setAll("ai-status-on");
            aiModelLabel.setText(config.assistant().model());
            stopAiPolling();
        } else {
            aiStatusIndicator.getStyleClass().setAll("ai-status-off");
            aiModelLabel.setText(messages.getString("ai.model.unavailable"));
            startAiPolling();
        }
    }

    private void startAiPolling() {
        if (aiPolling) return;
        aiPolling = true;
        aiScheduler = Executors.newSingleThreadScheduledExecutor();
        aiScheduler.scheduleAtFixedRate(
                this::checkAiAvailability,
                5, 5, TimeUnit.SECONDS
        );
    }

    private void stopAiPolling() {
        if (aiScheduler != null) {
            aiScheduler.shutdown();
            aiScheduler = null;
        }
        aiPolling = false;
    }

    private void checkForUpdates() {
        VersionChecker.UpdateInfo update = versionChecker.check();
        if (update.updateAvailable()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(messages.getString("update.title"));
            alert.setHeaderText(MessageFormat.format(messages.getString("update.header"), update.version()));
            alert.setContentText(messages.getString("update.content"));
            ButtonType download = new ButtonType(messages.getString("update.download"));
            ButtonType later = new ButtonType(messages.getString("update.later"));
            alert.getButtonTypes().setAll(download, later);
            alert.showAndWait().ifPresent(btn -> {
                if (btn == download) {
                    try {
                        java.awt.Desktop.getDesktop().browse(URI.create(update.url()));
                    } catch (IOException e) {
                        log.error("Failed to open browser", e);
                    }
                }
            });
        }
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
            if (updatingPrompt) return;
            String selected = taskCombo.getValue();
            if (selected == null || selected.equals(messages.getString("task.custom"))) {
                return;
            }
            if (selected.equals("──────────")) return;

            String prompt = templateManager.getPromptForTask(selected, messages);
            if (prompt != null) {
                updatingPrompt = true;
                promptField.setText(prompt);
                updatingPrompt = false;
            }
        });

        promptField.textProperty().addListener((obs, old, val) -> {
            if (updatingPrompt) return;
            schedulePromptSave();
            Platform.runLater(() -> {
                String selected = taskCombo.getValue();
                String expectedPrompt = templateManager.getPromptForTask(selected, messages);
                if (expectedPrompt == null || !expectedPrompt.equals(val)) {
                    taskCombo.setValue(messages.getString("task.custom"));
                }
            });
        });
    }

    private void schedulePromptSave() {
        if (saveTimer != null) {
            saveTimer.cancel();
        }
        saveTimer = new Timer(true);
        saveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> savePromptToConfig());
            }
        }, 500);
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
                updatingPrompt = true;
                promptField.clear();
                updatingPrompt = false;
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
    private void onOpenSourceFolder() { openFolder(sourcePathField.getEditor().getText()); }

    @FXML
    private void onOpenOutputFolder() { openFolder(outputPathField.getText()); }

    @FXML
    private void onStartStop() {
        if (running) {
            pipelineRunner.cancel();
            setStatusBar(messages.getString("status.cancelling"));
            setRunning(false);
            return;
        }

        String sourcePath = sourcePathField.getEditor().getText();
        sourcePathField.setValue(sourcePath);
        projectInfo = ProjectInfo.from(sourcePath);
        updateOutputPathWithProjectName();

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

    private void startScanTask(PrepareRequest request) {
        stopServerIfRunning();
        setRunning(true);
        progressBar.setVisible(true);
        fileTreeController.clear();
        chunkCardWindow.clear();

        pipelineRunner.run(
                request,
                config.oneFilePerChunk(),
                this::setStatusBar,
                this::addChunkCard,
                () -> {
                    progressBar.setVisible(false);
                    setRunning(false);
                    lastRequest = request;
                },
                files -> fileTreeController.populate(files, projectInfo.name(), Path.of(request.sourcePath())),
                this::setStatusBar
        );
    }

    private void addChunkCard(Path file) {
        chunkCardWindow.addChunkCard(file);
    }

    private void setRunning(boolean running) {
        this.running = running;
        if (running) {
            startStopButton.setText(messages.getString("stop.button"));
            startStopButton.getStyleClass().setAll("stop-button");
        } else {
            startStopButton.setText(messages.getString("start.button"));
            startStopButton.getStyleClass().setAll("start-button");
        }
    }

    @FXML
    private void onToggleChunkCardsWindow() {
        chunkCardWindow.toggle(getMainStage());
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
            serverButton.getStyleClass().setAll("server-button-on");
        } else {
            serverButton.setText(messages.getString("server.start.button"));
            serverButton.getStyleClass().setAll("server-button-off");
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
                outputPathField.setText(config.paths().outputPath().toString());
                logWindowManager.toggle(config.debugMode(), getMainStage());
                sourcePathField.getItems().setAll(projectHistory.getAll());
                buildTaskCombo();
                checkAiAvailability();
                setStatusBar(messages.getString("status.saved"));
            }
        } catch (IOException e) {
            log.error("Failed to open settings", e);
        }
    }

    @FXML
    private void onOpenInBrowser() {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://localhost:9090/project"));
        } catch (IOException e) {
            log.error("Failed to open browser", e);
            setStatusBar(messages.getString("status.browser.open.error"), true);
        }
    }

    @FXML
    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(messages.getString("help.title"));
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(helpService.buildHelpText(messages));
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(550, 450);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
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
        AppConfig freshConfig = configPort.load();
        if (prompt != null && !prompt.equals(freshConfig.prompt().systemPrompt())) {
            config = freshConfig.withPrompt(new PromptConfig(
                    prompt,
                    freshConfig.prompt().partPrefixTemplate(),
                    freshConfig.prompt().finalPartTemplate(),
                    freshConfig.prompt().fileSeparator(),
                    freshConfig.prompt().customTemplates()
            ));
            configPort.save(config);

            if (serverLauncher.isRunning()) {
                serverLauncher.updatePrompt(config.prompt());
            }

            log.debug("Prompt saved to config");
        }
    }

    private void setStatusBar(String text) { setStatusBar(text, false); }
    private void setStatusBar(ValidationError error) { setStatusBar(error.description(), true); }

    private void setStatusBar(String text, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: gray;");
        });
    }

    private Stage getMainStage() { return (Stage) sourcePathField.getScene().getWindow(); }

    private void disableDebugMode() {
        config = config.withDebugMode(false);
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
                String.valueOf(config.effectiveLimit())
        );
    }

    private void updateOutputPathWithProjectName() {
        outputPathField.setText(ProjectPathResolver.resolveOutputPath(
                projectInfo, config.paths().outputPath().toString()));
    }

    @FXML
    private void onImprovePrompt() {
        executeAssistant(promptAssistant::improve, "Введите промпт", "Промпт улучшен", "Не удалось улучшить промпт");
    }

    @FXML
    private void onExpandPrompt() {
        executeAssistant(promptAssistant::expand, "Введите короткую фразу", "Промпт развёрнут", "Не удалось развернуть промпт");
    }

    @FXML
    private void onGenerateTemplateName() {
        String text = promptField.getText();
        if (text == null || text.isBlank()) {
            setStatusBar("Введите промпт");
            return;
        }

        checkAiAvailabilityNow();

        if (!promptAssistant.isAvailable()) {
            setStatusBar("Локальная модель недоступна", true);
            return;
        }

        setStatusBar("Модель думает...");

        new Thread(() -> {
            String name = promptAssistant.generateName(text);
            Platform.runLater(() -> {
                if (!name.isBlank()) {
                    templateManager.saveOrUpdateTemplate(name, text);
                    buildTaskCombo();
                    taskCombo.setValue(name);
                    setStatusBar("Шаблон сохранён: " + name);
                } else {
                    setStatusBar("Не удалось придумать имя", true);
                }
            });
        }).start();
    }

    private void executeAssistant(java.util.function.Function<String, String> operation,
                                  String emptyTextWarning,
                                  String successMessage,
                                  String failureMessage) {
        String text = promptField.getText();
        if (text == null || text.isBlank()) {
            setStatusBar(emptyTextWarning);
            return;
        }

        checkAiAvailabilityNow();

        if (!promptAssistant.isAvailable()) {
            setStatusBar("Локальная модель недоступна", true);
            return;
        }

        setStatusBar("Модель думает...");

        new Thread(() -> {
            String result = operation.apply(text);
            Platform.runLater(() -> {
                if (!result.isBlank()) {
                    updatingPrompt = true;
                    promptField.setText(result);
                    promptField.positionCaret(0);
                    updatingPrompt = false;
                    setStatusBar(successMessage);
                } else {
                    setStatusBar(failureMessage, true);
                }
            });
        }).start();
    }
}