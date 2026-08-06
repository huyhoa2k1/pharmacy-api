package com.howie.pharmacy.pharmacy_store.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howie.pharmacy.pharmacy_store.entity.Notification;
import com.howie.pharmacy.pharmacy_store.repository.NotificationRepository;
import com.howie.pharmacy.pharmacy_store.services.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Notification createNotification(String message, Object data) {
        String dataJson = null;
        try {
            dataJson = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            dataJson = "{}";
        }

        Notification notify = new Notification();
        notify.setMessage(message);
        notify.setData(dataJson);
        notify.setIsReaded(false);

        Notification saved = notificationRepository.save(notify);

        // send to websocket topic for admin clients
        try {
            messagingTemplate.convertAndSend("/topic/notifications", saved);
        } catch (Exception ex) {
            // swallow to avoid breaking order creation flow
            System.err.println("Failed to send websocket notification: " + ex.getMessage());
        }

        return saved;
    }

    @Override
    public java.util.List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public java.util.List<Notification> markAllRead() {
        java.util.List<Notification> all = notificationRepository.findAll();
        for (Notification n : all) {
            if (n.getIsReaded() == null || !n.getIsReaded()) {
                n.setIsReaded(true);
            }
        }
        return notificationRepository.saveAll(all);
    }
}
