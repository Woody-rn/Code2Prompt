package ru.npepub.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.ai.AssistantConfig;
import ru.npepub.config.*;
import ru.npepub.di.api.C2PInject;
import ru.npepub.ui.util.UiResources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Controller for the settings dialog.
 */
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @FXML
    private ComboBox<String> modelCombo;
    @FXML
    private TextField maxSymbolsField;
    @FXML
    private TextField safetyMarginField;
    @FXML
    private TextField defaultOutputPathField;
    @FXML
    private ComboBox<String> devLogLevelCombo;
    @FXML
    private CheckBox debugModeCheckBox;
    @FXML
    private CheckBox oneFilePerChunkCheckBox;
    @FXML
    private ListView<String> excludedPatternsList;
    @FXML
    private TextField newPatternField;
    @FXML
    private TextField partPrefixField;
    @FXML
    private TextField finalPartField;
    @FXML
    private TextField fileSeparatorField;
    @FXML
    private CheckBox assistantEnabledCheckBox;
    @FXML
    private TextField assistantEndpointField;
    @FXML
    private TextField assistantModelField;

    @C2PInject
    private ConfigPort configPort;
    @C2PInject
    private ConfigJsonExporter jsonExporter;

    private AppConfig config;
    private ObservableList<String> excludedPatterns;
    private ResourceBundle messages;

    private static final Map<String, Integer> MODEL_LIMITS = Map.of(
            "DeepSeek V3", 100_000,
            "GPT-4o", 128_000,
            "GPT-4 Turbo", 128_000,
            "Claude 3.5 Sonnet", 200_000,
            "Gemini 1.5 Pro", 1_000_000
    );

    @FXML
    public void initialize() {
        messages = UiResources.getBundle();
        config = configPort.load();

        modelCombo.getItems().addAll(MODEL_LIMITS.keySet());
        modelCombo.setOnAction(e -> {
            String selected = modelCombo.getValue();
            Integer limit = MODEL_LIMITS.get(selected);
            if (limit != null) maxSymbolsField.setText(String.valueOf(limit));
        });

        devLogLevelCombo.getItems().addAll("DEBUG", "INFO", "WARN", "OFF");

        excludedPatterns = FXCollections.observableArrayList();
        excludedPatternsList.setItems(excludedPatterns);
        excludedPatternsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = excludedPatternsList.getSelectionModel().getSelectedItem();
                if (selected != null) excludedPatterns.remove(selected);
            }
        });

        setupNumericFields();

        fillForm(config);
    }

    /**
     * Restricts input to valid numeric formats.
     * maxSymbolsField accepts up to 7 digits (max 9,999,999 — fits in int).
     * safetyMarginField accepts 0-100 (percent).
     */
    private void setupNumericFields() {
        Pattern digits = Pattern.compile("\\d{0,7}");
        Pattern percent = Pattern.compile("\\d{0,2}|100");

        maxSymbolsField.setTextFormatter(new TextFormatter<>(change ->
                digits.matcher(change.getControlNewText()).matches() ? change : null));

        safetyMarginField.setTextFormatter(new TextFormatter<>(change ->
                percent.matcher(change.getControlNewText()).matches() ? change : null));
    }

    private void fillForm(AppConfig config) {
        modelCombo.setValue(config.aiModel().name());
        maxSymbolsField.setText(String.valueOf(config.aiModel().maxSymbols()));
        safetyMarginField.setText(String.valueOf((int) (config.aiModel().safetyMargin() * 100)));
        defaultOutputPathField.setText(config.paths().outputPath().toString());
        devLogLevelCombo.setValue(config.log().level().name());
        debugModeCheckBox.setSelected(config.debugMode());
        oneFilePerChunkCheckBox.setSelected(config.oneFilePerChunk());
        excludedPatterns.setAll(mergeExclusions(config));
        partPrefixField.setText(config.prompt().partPrefixTemplate());
        finalPartField.setText(config.prompt().finalPartTemplate());
        fileSeparatorField.setText(config.prompt().fileSeparator());
        assistantEnabledCheckBox.setSelected(config.assistant().enabled());
        assistantEndpointField.setText(config.assistant().endpoint());
        assistantModelField.setText(config.assistant().model());
    }

    private List<String> mergeExclusions(AppConfig config) {
        List<String> all = new ArrayList<>();
        config.filter().excludedDirs().stream().sorted().forEach(d -> {
            String name = d.endsWith("/") ? d : d + "/";
            all.add(name);
        });
        config.filter().excludedFileNames().stream().sorted().forEach(all::add);
        config.filter().patterns().stream().sorted().forEach(all::add);
        return all;
    }

    @FXML
    private void onAddPattern() {
        String input = newPatternField.getText().trim();
        if (input.isEmpty() || excludedPatterns.contains(input)) return;

        excludedPatterns.add(input);
        FXCollections.sort(excludedPatterns);
        newPatternField.clear();
    }

    @FXML
    private void onBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(messages.getString("browse.output.title"));
        File initialDir = new File(defaultOutputPathField.getText());
        if (initialDir.exists() && initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }
        File dir = chooser.showDialog(defaultOutputPathField.getScene().getWindow());
        if (dir != null) defaultOutputPathField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onResetToDefaults() {
        fillForm(AppConfig.defaults());
    }

    @FXML
    private void onExportConfig() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(messages.getString("export.title"));
        chooser.setInitialFileName("code2prompt-config.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = chooser.showSaveDialog(partPrefixField.getScene().getWindow());
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), jsonExporter.toJson(getUpdatedConfig()));
        } catch (IOException e) {
            log.error("Failed to export config", e);
        }
    }

    @FXML
    private void onImportConfig() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(messages.getString("import.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = chooser.showOpenDialog(partPrefixField.getScene().getWindow());
        if (file == null) return;
        try {
            String json = Files.readString(file.toPath());
            AppConfig imported = jsonExporter.fromJson(json);
            this.config = imported;
            fillForm(imported);
        } catch (Exception e) {
            log.error("Failed to import config", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(messages.getString("import.error.title"));
            alert.setHeaderText(messages.getString("import.error.header"));
            alert.setContentText(messages.getString("import.error.content"));
            alert.showAndWait();
        }
    }

    @FXML
    private void onOpenLogs() {
        try {
            Path logDir = Code2PromptPaths.LOG_DIR;
            java.awt.Desktop.getDesktop().open(logDir.toFile());
        } catch (IOException e) {
            log.error("Failed to open logs folder", e);
        }
    }

    /**
     * Returns updated config from the form values.
     */
    public AppConfig getUpdatedConfig() {
        Set<String> dirs = new HashSet<>();
        Set<String> files = new HashSet<>();
        Set<String> patterns = new HashSet<>();

        for (String item : excludedPatterns) {
            if (item.endsWith("/")) {
                dirs.add(item.substring(0, item.length() - 1));
            } else if (item.contains("*")) {
                patterns.add(item);
            } else {
                files.add(item);
            }
        }

        return new AppConfig(
                new ModelLimitConfig(
                        modelCombo.getValue(),
                        Integer.parseInt(maxSymbolsField.getText()),
                        Double.parseDouble(safetyMarginField.getText()) / 100.0
                ),
                new PathConfig(
                        Path.of(defaultOutputPathField.getText())
                ),
                new FilterConfig(dirs, files, patterns),
                new LogConfig(
                        LogConfig.LogLevel.valueOf(devLogLevelCombo.getValue()),
                        config.log().errorEnabled()
                ),
                new PromptConfig(
                        config.prompt().systemPrompt(),
                        partPrefixField.getText(),
                        finalPartField.getText(),
                        fileSeparatorField.getText(),
                        config.prompt().customTemplates()
                ),
                new AssistantConfig(
                        assistantEndpointField.getText(),
                        assistantModelField.getText(),
                        assistantEnabledCheckBox.isSelected()
                ),
                oneFilePerChunkCheckBox.isSelected(),
                debugModeCheckBox.isSelected()
        );
    }
}