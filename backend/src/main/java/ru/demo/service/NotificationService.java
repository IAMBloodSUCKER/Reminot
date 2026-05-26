package ru.demo.service;

import ru.demo.model.Notification;
import ru.demo.repository.NotificationRepositoryInterface;
import ru.demo.storage.AppEventLogger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private static final Duration MAX_OVERDUE_TO_FIRE = Duration.ofHours(24);

    private final NotificationRepositoryInterface repository;
    private final Consumer<Notification> onNotificationFired;
    private final Consumer<Notification> onTrayInteraction;
    private final ScheduledExecutorService scheduler;

    private TrayIcon trayIcon;
    private volatile Notification lastTrayNotification;
    private volatile boolean started;

    public NotificationService(
            NotificationRepositoryInterface repository,
            Consumer<Notification> onNotificationFired,
            Consumer<Notification> onTrayInteraction
    ) {
        this.repository = repository;
        this.onNotificationFired = onNotificationFired;
        this.onTrayInteraction = Objects.requireNonNullElse(onTrayInteraction, notification -> {
        });
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "reminot-notification-poller");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        initTrayIcon();
        scheduler.scheduleAtFixedRate(this::pollNotifications, 0, 1, TimeUnit.SECONDS);
        AppEventLogger.info("NotificationService started");
        LOGGER.info("NotificationService started");
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        started = false;
        scheduler.shutdownNow();
        removeTrayIcon();
        AppEventLogger.info("NotificationService stopped");
        LOGGER.info("NotificationService stopped");
    }

    public boolean isTrayAvailable() {
        return trayIcon != null;
    }

    private void pollNotifications() {
        try {
            LocalDateTime now = LocalDateTime.now().withNano(0);
            List<Notification> active = repository.allActiveNotification();
            for (Notification notification : active) {
                if (notification.getTriggerAt() == null || notification.getMessage() == null) {
                    continue;
                }
                if (!notification.isActive()) {
                    continue;
                }

                if (!now.isBefore(notification.getTriggerAt())) {
                    Duration overdue = Duration.between(notification.getTriggerAt(), now);
                    if (overdue.compareTo(MAX_OVERDUE_TO_FIRE) > 0) {
                        notification.setActive(false);
                        repository.updateNotification(notification);
                        AppEventLogger.event("Notification skipped as stale id=" + notification.getId());
                        continue;
                    }

                    notification.setActive(false);
                    repository.updateNotification(notification);
                    sendWindowsNotification(notification);
                    onNotificationFired.accept(notification);
                    AppEventLogger.event("Notification fired id=" + notification.getId());
                }
            }
        } catch (Exception ex) {
            AppEventLogger.error("Error while polling notifications", ex);
            LOGGER.log(Level.WARNING, "Error while polling notifications", ex);
        }
    }

    private void sendWindowsNotification(Notification notification) {
        String message = notification.getMessage();
        if (trayIcon != null) {
            lastTrayNotification = notification;
            trayIcon.displayMessage("Reminot", message, TrayIcon.MessageType.INFO);
            return;
        }
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                null,
                message,
                "Reminot",
                JOptionPane.INFORMATION_MESSAGE
        ));
    }

    private void initTrayIcon() {
        if (!SystemTray.isSupported()) {
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            trayIcon = new TrayIcon(createDummyImage(), "Reminot");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Reminot notifications");
            trayIcon.setActionCommand("open-reminot");
            trayIcon.addActionListener(e -> {
                Notification source = lastTrayNotification;
                lastTrayNotification = null;
                SwingUtilities.invokeLater(() -> onTrayInteraction.accept(source));
            });

            PopupMenu popupMenu = new PopupMenu();
            MenuItem openItem = new MenuItem("Open Reminot");
            openItem.addActionListener(e -> SwingUtilities.invokeLater(() -> onTrayInteraction.accept(null)));
            popupMenu.add(openItem);
            trayIcon.setPopupMenu(popupMenu);
            tray.add(trayIcon);
        } catch (AWTException ex) {
            trayIcon = null;
            LOGGER.log(Level.WARNING, "Cannot initialize tray icon", ex);
        }
    }

    private void removeTrayIcon() {
        if (trayIcon == null || !SystemTray.isSupported()) {
            return;
        }
        SystemTray.getSystemTray().remove(trayIcon);
        trayIcon = null;
        lastTrayNotification = null;
    }

    private Image createDummyImage() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 16, 16);
        g2.setColor(Color.WHITE);
        g2.drawString("R", 4, 12);
        g2.dispose();
        return img;
    }
}
