package com.howie.pharmacy.pharmacy_store.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

final class ChatRequestParser {

    private ChatRequestParser() {
    }

    static String latestUserMessage(JsonNode messages) {
        if (!messages.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Messages are required");
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            JsonNode message = messages.get(index);
            if (!"user".equals(message.path("role").asText())) {
                continue;
            }
            String content = message.path("content").asText(null);
            if (content != null && !content.isBlank()) {
                return content;
            }
            JsonNode parts = message.path("parts");
            if (parts.isArray()) {
                StringBuilder text = new StringBuilder();
                for (JsonNode part : parts) {
                    if ("text".equals(part.path("type").asText())) {
                        JsonNode partContent = part.path("content");
                        if (partContent.isTextual()) {
                            text.append(partContent.asText());
                        }
                    }
                }
                if (!text.isEmpty()) {
                    return text.toString();
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A user message is required");
    }
}