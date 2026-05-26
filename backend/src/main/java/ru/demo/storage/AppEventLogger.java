package ru.demo.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AppEventLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppEventLogger() {
    }

    public static void event(String text) {
        append(LocalStoragePaths.eventsFile(), "[EVENT] " + text);
    }

    public static void info(String text) {
        append(LocalStoragePaths.appLogFile(), "[INFO] " + text);
    }

    public static void error(String text, Throwable error) {
        String message = "[ERROR] " + text + (error == null ? "" : " :: " + error.getMessage());
        append(LocalStoragePaths.appLogFile(), message);
    }

    private static synchronized void append(Path path, String message) {
        String line = TS.format(LocalDateTime.now()) + " " + message + System.lineSeparator();
        try {
            Files.writeString(
                    path,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }
}
