package com.howie.pharmacy.pharmacy_store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.howie.pharmacy.pharmacy_store.entity.ChatConversation;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    List<ChatConversation> findAllByUser_IdOrderByUpdatedAtDesc(Integer userId);

    Optional<ChatConversation> findByIdAndUser_Id(String id, Integer userId);
}