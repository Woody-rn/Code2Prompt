package ru.npepub.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import ru.npepub.config.AppConfig;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.api.C2PInject;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Controller for the settings dialog.
 */
public class SettingsController {

    @FXML private ComboBox<String> modelCombo;
    @FXML private TextField maxSymbolsField;
    @FXML private TextField safetyMarginField;
    @FXML private TextField defaultOutputPathField;
    @FXML private ComboBox<String> devLogLevelCombo;
    @FXML private CheckBox debugModeCheckBox;
    @FXML private ListView<String> excludedDirsList;
    @FXML private TextField newExcludedDirField;

    @SuppressWarnings("unused")
    @C2PInject
    private ConfigPort configPort;

    private AppConfig config;
    private ObservableList<String> excludedDirs;

    private static final Map<String, Integer> MODEL_LIMITS = Map.of(
            "DeepSeek V3", 100_000,
            "GPT-4o", 128_000,
            "GPT-4 Turbo", 128_000,
            "Claude 3.5 Sonnet", 200_000,
            "Gemini 1.5 Pro", 1_000_000
    );

    @FXML
    public void initialize() {
        config = configPort.load();

        modelCombo.getItems().addAll(MODEL_LIMITS.keySet());
        modelCombo.setValue(config.modelName());

        modelCombo.setOnAction(e -> {
            String selected = modelCombo.getValue();
            Integer limit = MODEL_LIMITS.get(selected);
            if (limit != null) {
                maxSymbolsField.setText(String.valueOf(limit));
            }
        });

        maxSymbolsField.setText(String.valueOf(config.maxSymbols()));
        safetyMarginField.setText(String.valueOf((int) (config.safetyMargin() * 100)));
        defaultOutputPathField.setText(config.outputPath().toString());

        debugModeCheckBox.setSelected(config.debugMode());

        devLogLevelCombo.getItems().addAll("DEBUG", "INFO", "WARN", "OFF");
        devLogLevelCombo.setValue(config.logLevel().name());

        excludedDirs = FXCollections.observableArrayList(
                config.excludedDirs().stream().sorted().toList()
        );
        excludedDirsList.setItems(excludedDirs);

        excludedDirsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = excludedDirsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    excludedDirs.remove(selected);
                }
            }
        });
    }

    @FXML
    private void onBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку");
        File initialDir = new File(defaultOutputPathField.getText());
        if (initialDir.exists() && initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }
        File dir = chooser.showDialog(defaultOutputPathField.getScene().getWindow());
        if (dir != null) {
            defaultOutputPathField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void onAddExcludedDir() {
        String dir = newExcludedDirField.getText().trim();
        if (!dir.isEmpty() && !excludedDirs.contains(dir)) {
            excludedDirs.add(dir);
            newExcludedDirField.clear();
        }
    }

    /**
     * @return updated config from the form values
     */
    public AppConfig getUpdatedConfig() {
        return new AppConfig(
                modelCombo.getValue(),
                Integer.parseInt(maxSymbolsField.getText()),
                Double.parseDouble(safetyMarginField.getText()) / 100.0,
                Path.of(defaultOutputPathField.getText()),
                AppConfig.LogLevel.valueOf(devLogLevelCombo.getValue()),
                config.errorLogEnabled(),
                debugModeCheckBox.isSelected(),
                config.recentProjects(),
                config.recentProjectsCount(),
                List.copyOf(excludedDirs)
        );
    }
}