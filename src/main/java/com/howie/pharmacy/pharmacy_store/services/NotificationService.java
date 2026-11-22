package com.howie.pharmacy.pharmacy_store.services;

import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;
import com.howie.pharmacy.pharmacy_store.entity.Notification;
import java.util.List;

import org.springframework.data.domain.Page;

public interface NotificationService {
    Notification createNotification(String message, Object data);

    Page<Notification> getAllNotifications(ListRequest request);

    java.util.List<Notification> markAllRead();
}
