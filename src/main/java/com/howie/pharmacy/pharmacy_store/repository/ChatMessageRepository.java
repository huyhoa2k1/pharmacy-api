package com.howie.pharmacy.pharmacy_store.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.howie.pharmacy.pharmacy_store.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop20ByConversation_IdOrderByCreatedAtDesc(String conversationId);

    List<ChatMessage> findAllByConversation_IdOrderByCreatedAtAsc(String conversationId);
}