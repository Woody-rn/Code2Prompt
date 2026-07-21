package ru.npepub.ui;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.ConfigPort;
import ru.npepub.di.C2PInject;
import ru.npepub.model.AppConfig;

import java.io.File;
import java.nio.file.Path;

/**
 * Controller for the settings dialog.
 */
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @FXML private ComboBox<String> modelCombo;
    @FXML private TextField maxSymbolsField;
    @FXML private TextField safetyMarginField;
    @FXML private TextField defaultOutputPathField;
    @FXML private ComboBox<String> logLevelCombo;

    @C2PInject
    private ConfigPort configPort;

    private AppConfig config;

    @FXML
    public void initialize() {
        config = configPort.load();

        modelCombo.getItems().addAll("DeepSeek V3", "GPT-4o", "GPT-4 Turbo", "Claude 3.5 Sonnet", "Gemini 1.5 Pro");
        modelCombo.setValue(config.modelName());

        maxSymbolsField.setText(String.valueOf(config.maxSymbols()));
        safetyMarginField.setText(String.valueOf((int) (config.safetyMargin() * 100)));

        defaultOutputPathField.setText(config.outputPath().toString());

        logLevelCombo.getItems().addAll("DEBUG", "INFO", "WARN", "OFF");
        logLevelCombo.setValue(config.logLevel().name());
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

    /**
     * @return updated config from the form values
     */
    public AppConfig getUpdatedConfig() {
        return new AppConfig(
                modelCombo.getValue(),
                Integer.parseInt(maxSymbolsField.getText()),
                Double.parseDouble(safetyMarginField.getText()) / 100.0,
                Path.of(defaultOutputPathField.getText()),
                AppConfig.LogLevel.valueOf(logLevelCombo.getValue()),
                config.errorLogEnabled()
        );
    }
}