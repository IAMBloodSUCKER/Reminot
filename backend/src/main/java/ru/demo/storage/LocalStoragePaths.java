package ru.demo.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class LocalStoragePaths {

    private static final String APP_DIR_NAME = "Reminot";

    private LocalStoragePaths() {
    }

    public static Path baseDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = (localAppData != null && !localAppData.isBlank())
                ? Paths.get(localAppData, APP_DIR_NAME)
                : Paths.get(System.getProperty("user.home"), "." + APP_DIR_NAME.toLowerCase());
        ensureDirectory(base);
        return base;
    }

    public static Path notificationsDbFile() {
        return baseDir().resolve("notifications.db.txt");
    }

    public static Path eventsFile() {
        return baseDir().resolve("events.log");
    }

    public static Path appLogFile() {
        return baseDir().resolve("app.log");
    }

    private static void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory: " + dir, e);
        }
    }
}
