package com.howie.pharmacy.pharmacy_store.controllers;

import com.howie.pharmacy.pharmacy_store.entity.Notification;
import com.howie.pharmacy.pharmacy_store.services.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> getAll() {
        return notificationService.getAllNotifications();
    }

    @PostMapping("/mark-all-read")
    public List<Notification> markAllRead() {
        return notificationService.markAllRead();
    }
}
