package ru.npepub.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.*;
import ru.npepub.di.api.C2PInject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the settings dialog.
 */
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @FXML private ComboBox<String> modelCombo;
    @FXML private TextField maxSymbolsField;
    @FXML private TextField safetyMarginField;
    @FXML private TextField defaultOutputPathField;
    @FXML private ComboBox<String> devLogLevelCombo;
    @FXML private CheckBox debugModeCheckBox;
    @FXML private ListView<String> excludedPatternsList;
    @FXML private TextField newPatternField;
    @FXML private TextField partPrefixField;
    @FXML private TextField finalPartField;
    @FXML private TextField fileSeparatorField;

    @C2PInject private ConfigPort configPort;
    @C2PInject private ConfigJsonExporter jsonExporter;

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
        messages = ResourceBundle.getBundle("messages");
        config = configPort.load();

        modelCombo.getItems().addAll(MODEL_LIMITS.keySet());
        modelCombo.setValue(config.aiModel().name());

        modelCombo.setOnAction(e -> {
            String selected = modelCombo.getValue();
            Integer limit = MODEL_LIMITS.get(selected);
            if (limit != null) maxSymbolsField.setText(String.valueOf(limit));
        });

        maxSymbolsField.setText(String.valueOf(config.aiModel().maxSymbols()));
        safetyMarginField.setText(String.valueOf((int)(config.aiModel().safetyMargin() * 100)));
        defaultOutputPathField.setText(config.paths().outputPath().toString());

        debugModeCheckBox.setSelected(config.debugMode());

        devLogLevelCombo.getItems().addAll("DEBUG", "INFO", "WARN", "OFF");
        devLogLevelCombo.setValue(config.log().level().name());

        excludedPatterns = FXCollections.observableArrayList(mergeExclusions());
        excludedPatternsList.setItems(excludedPatterns);
        excludedPatternsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = excludedPatternsList.getSelectionModel().getSelectedItem();
                if (selected != null) excludedPatterns.remove(selected);
            }
        });

        partPrefixField.setText(config.prompt().partPrefixTemplate());
        finalPartField.setText(config.prompt().finalPartTemplate());
        fileSeparatorField.setText(config.prompt().fileSeparator());
    }

    private List<String> mergeExclusions() {
        List<String> all = new ArrayList<>();
        config.filter().excludedDirs().stream().sorted().forEach(d -> all.add(d + "/"));
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
        AppConfig defaults = AppConfig.defaults();
        modelCombo.setValue(defaults.aiModel().name());
        maxSymbolsField.setText(String.valueOf(defaults.aiModel().maxSymbols()));
        safetyMarginField.setText(String.valueOf((int)(defaults.aiModel().safetyMargin() * 100)));
        defaultOutputPathField.setText(defaults.paths().outputPath().toString());
        devLogLevelCombo.setValue(defaults.log().level().name());
        debugModeCheckBox.setSelected(defaults.debugMode());
        excludedPatterns.setAll(
                mergeDefaults(
                        defaults.filter().excludedDirs(),
                        defaults.filter().excludedFileNames(),
                        defaults.filter().patterns()
                )
        );
        partPrefixField.setText(defaults.prompt().partPrefixTemplate());
        finalPartField.setText(defaults.prompt().finalPartTemplate());
        fileSeparatorField.setText(defaults.prompt().fileSeparator());
    }

    private List<String> mergeDefaults(List<String> dirs, List<String> files, List<String> patterns) {
        List<String> all = new ArrayList<>();
        dirs.stream().sorted().forEach(d -> all.add(d + "/"));
        files.stream().sorted().forEach(all::add);
        patterns.stream().sorted().forEach(all::add);
        return all;
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
            applyConfig(jsonExporter.fromJson(json));
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
            Path logDir = Path.of(System.getProperty("user.home"), ".code2prompt", "logs");
            java.awt.Desktop.getDesktop().open(logDir.toFile());
        } catch (IOException e) {
            log.error("Failed to open logs folder", e);
        }
    }

    private void applyConfig(AppConfig config) {
        modelCombo.setValue(config.aiModel().name());
        maxSymbolsField.setText(String.valueOf(config.aiModel().maxSymbols()));
        safetyMarginField.setText(String.valueOf((int)(config.aiModel().safetyMargin() * 100)));
        defaultOutputPathField.setText(config.paths().outputPath().toString());
        devLogLevelCombo.setValue(config.log().level().name());
        debugModeCheckBox.setSelected(config.debugMode());
        excludedPatterns.setAll(
                mergeDefaults(
                        config.filter().excludedDirs(),
                        config.filter().excludedFileNames(),
                        config.filter().patterns()
                )
        );
        partPrefixField.setText(config.prompt().partPrefixTemplate());
        finalPartField.setText(config.prompt().finalPartTemplate());
        fileSeparatorField.setText(config.prompt().fileSeparator());
    }

    /** Returns updated config from the form values. */
    public AppConfig getUpdatedConfig() {
        List<String> dirs = new ArrayList<>();
        List<String> files = new ArrayList<>();
        List<String> patterns = new ArrayList<>();

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
                new AiModelConfig(
                        modelCombo.getValue(),
                        Integer.parseInt(maxSymbolsField.getText()),
                        Double.parseDouble(safetyMarginField.getText()) / 100.0
                ),
                new PathConfig(
                        Path.of(defaultOutputPathField.getText()),
                        config.paths().recentProjects(),
                        config.paths().recentProjectsCount()
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
                debugModeCheckBox.isSelected()
        );
    }
}