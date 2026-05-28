package ru.demo.i18n;

import ru.demo.storage.LocalStoragePaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class LanguageStore {

    private static final String FILE_NAME = "ui.properties";
    private static final String KEY_LANGUAGE = "language";

    private LanguageStore() {
    }

    public static AppLanguage load() {
        Path file = preferencesFile();
        if (!Files.isRegularFile(file)) {
            return AppLanguage.RU;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            return AppLanguage.RU;
        }
        String raw = properties.getProperty(KEY_LANGUAGE, AppLanguage.RU.name());
        try {
            return AppLanguage.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AppLanguage.RU;
        }
    }

    public static void save(AppLanguage language) {
        Path file = preferencesFile();
        Properties properties = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            } catch (IOException ignored) {
                // keep empty properties
            }
        }
        properties.setProperty(KEY_LANGUAGE, language.name());
        try (OutputStream out = Files.newOutputStream(file)) {
            properties.store(out, "Reminot UI preferences");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save language preference: " + file, e);
        }
    }

    private static Path preferencesFile() {
        return LocalStoragePaths.baseDir().resolve(FILE_NAME);
    }
}
