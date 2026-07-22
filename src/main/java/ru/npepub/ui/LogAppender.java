package ru.npepub.ui;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Logback appender that writes log events to a JavaFX TextArea in real time.
 */
public class LogAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private static TextArea textArea;
    private static LogAppender instance;

    /**
     * Installs the appender to the root logger and starts writing to the given TextArea.
     */
    public static void install(TextArea area) {
        if (instance != null) return;
        textArea = area;

        instance = new LogAppender();
        instance.setContext((ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory());
        instance.start();

        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.addAppender(instance);
    }

    /**
     * Uninstalls the appender from the root logger.
     */
    public static void uninstall() {
        if (instance == null) return;
        instance.stop();
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.detachAppender(instance);
        instance = null;
        textArea = null;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (textArea == null) return;

        String timestamp = FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()));
        String line = String.format("%s %-5s %s - %s%n",
                timestamp,
                event.getLevel(),
                event.getLoggerName(),
                event.getFormattedMessage()
        );

        Platform.runLater(() -> textArea.appendText(line));
    }
}