package com.howie.pharmacy.pharmacy_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.howie.pharmacy.pharmacy_store.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

}
