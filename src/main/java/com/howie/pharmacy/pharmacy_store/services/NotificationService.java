package com.howie.pharmacy.pharmacy_store.services;

import com.howie.pharmacy.pharmacy_store.entity.Notification;
import java.util.List;

public interface NotificationService {
    Notification createNotification(String message, Object data);

    List<Notification> getAllNotifications();

    java.util.List<Notification> markAllRead();
}
