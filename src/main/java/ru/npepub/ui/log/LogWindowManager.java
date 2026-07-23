package ru.npepub.ui.log;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.npepub.di.C2PComponent;

/**
 * Manages a separate window that displays real-time logs.
 */
@C2PComponent
class LogWindowManager implements LogWindowPort {

    private Stage logStage;
    private LogViewPort logView;

    @Override
    public void show(Stage mainStage) {
        if (logStage != null) return;

        TextArea logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11;");

        logView = new LogAppender(logTextArea);
        logView.attach();

        logStage = new Stage();
        logStage.setTitle("Логи Code2Prompt");
        logStage.setScene(new Scene(new StackPane(logTextArea), 600, 450));

        bindToMainStage(mainStage);
        logStage.show();
    }

    @Override
    public void hide() {
        if (logStage != null) {
            logView.detach();
            logStage.close();
            logStage = null;
            logView = null;
        }
    }

    @Override
    public void toggle(boolean enabled, Stage mainStage) {
        if (enabled) {
            show(mainStage);
        } else {
            hide();
        }
    }

    @Override
    public boolean isShowing() {
        return logStage != null;
    }

    private void bindToMainStage(Stage mainStage) {
        logStage.setX(mainStage.getX() + mainStage.getWidth());
        logStage.setY(mainStage.getY());
        logStage.setHeight(mainStage.getHeight());
        mainStage.xProperty().addListener((obs, o, n) -> logStage.setX(n.doubleValue() + mainStage.getWidth()));
        mainStage.yProperty().addListener((obs, o, n) -> logStage.setY(n.doubleValue()));
        mainStage.heightProperty().addListener((obs, o, n) -> logStage.setHeight(n.doubleValue()));
    }
}