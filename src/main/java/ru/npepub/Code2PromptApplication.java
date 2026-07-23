package ru.npepub;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.ContainerDI;

import java.util.Objects;

public class Code2PromptApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(Code2PromptApplication.class);

    private ContainerDI container;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        log.info("Initializing DI container");
        container = new ContainerDI();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting Code2Prompt");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(container::createAndInject);

        Parent root = loader.load();
        primaryStage.setTitle("Code2Prompt - npepub.ru");
        primaryStage.setScene(new Scene(root));
        primaryStage.getIcons().add(new javafx.scene.image.Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))));
        primaryStage.show();

        log.info("Application started");
    }
}