package ru.demo.repository;

import ru.demo.model.Notification;
import ru.demo.storage.AppEventLogger;
import ru.demo.storage.LocalStoragePaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NotificationRepository implements NotificationRepositoryInterface {

    private final List<Notification> repositoryNotifications = new ArrayList<>();
    private final Path dbFile = LocalStoragePaths.notificationsDbFile();

    public NotificationRepository() {
        loadFromFile();
    }

    @Override
    public synchronized void addNotification(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        repositoryNotifications.add(notification);
        persist();
        AppEventLogger.event("Notification added id=" + notification.getId());
    }

    @Override
    public synchronized void updateNotification(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        Long id = notification.getId();
        if (id == null) {
            return;
        }
        for (int i = 0; i < repositoryNotifications.size(); i++) {
            Notification current = repositoryNotifications.get(i);
            if (id.equals(current.getId())) {
                current.setTriggerAt(notification.getTriggerAt());
                current.setMessage(notification.getMessage());
                current.setActive(notification.isActive());
                persist();
                AppEventLogger.event("Notification updated id=" + id + " active=" + notification.isActive());
                return;
            }
        }
        repositoryNotifications.add(notification);
        persist();
        AppEventLogger.event("Notification upsert add id=" + id);
    }

    @Override
    public synchronized void deleteNotificationById(Long id) {
        if (id == null) {
            return;
        }
        repositoryNotifications.removeIf(notification -> id.equals(notification.getId()));
        persist();
        AppEventLogger.event("Notification deleted id=" + id);
    }

    @Override
    public synchronized Notification getNotificationById(Long id) {
        if (id == null) {
            return null;
        }
        for (Notification notification : repositoryNotifications) {
            if (id.equals(notification.getId())) {
                return notification;
            }
        }
        return null;
    }

    @Override
    public synchronized List<Notification> allActiveNotification() {
        return repositoryNotifications.stream()
                .filter(Notification::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Notification> allNotification() {
        return Collections.unmodifiableList(new ArrayList<>(repositoryNotifications));
    }

    private void loadFromFile() {
        if (!Files.exists(dbFile)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(dbFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                Notification notification = deserialize(line);
                if (notification != null) {
                    repositoryNotifications.add(notification);
                }
            }
            AppEventLogger.info("Loaded notifications from file: " + repositoryNotifications.size());
        } catch (IOException e) {
            AppEventLogger.error("Cannot load notifications DB", e);
        }
    }

    private void persist() {
        List<String> lines = repositoryNotifications.stream()
                .map(this::serialize)
                .collect(Collectors.toList());
        try {
            Files.write(
                    dbFile,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            AppEventLogger.error("Cannot persist notifications DB", e);
        }
    }

    private String serialize(Notification notification) {
        String id = notification.getId() == null ? "" : notification.getId().toString();
        String triggerAt = notification.getTriggerAt() == null ? "" : notification.getTriggerAt().toString();
        String active = String.valueOf(notification.isActive());
        String message = notification.getMessage() == null ? "" : notification.getMessage();
        String encodedMessage = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
        return id + "|" + triggerAt + "|" + active + "|" + encodedMessage;
    }

    private Notification deserialize(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) {
            return null;
        }
        try {
            Notification notification = new Notification();
            notification.setId(parts[0].isBlank() ? null : Long.parseLong(parts[0]));
            notification.setTriggerAt(parseTriggerAt(parts[1]));
            notification.setActive(Boolean.parseBoolean(parts[2]));
            String message = new String(Base64.getDecoder().decode(parts[3]), StandardCharsets.UTF_8);
            notification.setMessage(message);
            return notification;
        } catch (Exception ex) {
            AppEventLogger.error("Cannot parse notification DB row", ex);
            return null;
        }
    }

    private LocalDateTime parseTriggerAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
        }
        try {
            LocalTime time = LocalTime.parse(raw);
            return LocalDate.now().atTime(time);
        } catch (Exception ex) {
            AppEventLogger.error("Cannot parse triggerAt value: " + raw, ex);
            return null;
        }
    }
}

