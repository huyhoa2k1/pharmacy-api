package com.howie.pharmacy.pharmacy_store.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(String id, String role, String content, LocalDateTime createdAt) {
}