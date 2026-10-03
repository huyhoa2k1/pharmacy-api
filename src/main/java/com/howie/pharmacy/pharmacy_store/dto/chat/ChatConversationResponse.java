package com.howie.pharmacy.pharmacy_store.dto.chat;

import java.time.LocalDateTime;

public record ChatConversationResponse(String id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {
}