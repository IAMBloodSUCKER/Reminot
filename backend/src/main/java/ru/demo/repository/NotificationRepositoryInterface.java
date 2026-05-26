package ru.demo.repository;

import ru.demo.model.Notification;

import java.util.List;

public interface NotificationRepositoryInterface {

    void addNotification(Notification notification);

    void updateNotification(Notification notification);

    void deleteNotificationById(Long id);

    Notification getNotificationById(Long id);

    List<Notification> allActiveNotification();

    List<Notification> allNotification();
}
