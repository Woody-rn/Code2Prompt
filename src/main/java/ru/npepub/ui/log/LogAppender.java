package ru.npepub.ui.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

class LogAppender extends AppenderBase<ILoggingEvent> implements LogViewPort {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private TextArea textArea;
    private Logger rootLogger;

    public LogAppender(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void attach() {
        rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        start();
        rootLogger.addAppender(this);
    }

    @Override
    public void detach() {
        if (rootLogger != null) {
            stop();
            rootLogger.detachAppender(this);
            rootLogger = null;
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (textArea == null) return;
        String timestamp = FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()));
        String line = String.format("%s %-5s %s - %s%n",
                timestamp, event.getLevel(), event.getLoggerName(), event.getFormattedMessage());
        Platform.runLater(() -> textArea.appendText(line));
    }
}