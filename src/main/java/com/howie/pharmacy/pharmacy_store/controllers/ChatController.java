package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.howie.pharmacy.pharmacy_store.dto.chat.ChatConversationResponse;
import com.howie.pharmacy.pharmacy_store.dto.chat.ChatMessageResponse;
import com.howie.pharmacy.pharmacy_store.security.UserPrincipal;
import com.howie.pharmacy.pharmacy_store.services.PharmacyChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final PharmacyChatService chatService;

    public ChatController(PharmacyChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversations")
    public ResponseEntity<ChatConversationResponse> createConversation(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createConversation(principal.getId()));
    }

    @GetMapping("/conversations")
    public List<ChatConversationResponse> listConversations(@AuthenticationPrincipal UserPrincipal principal) {
        return chatService.listConversations(principal.getId());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessageResponse> getMessages(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String conversationId) {
        return chatService.getMessages(principal.getId(), conversationId);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String conversationId) {
        chatService.deleteConversation(principal.getId(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public Map<String, Object> hydrate(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String threadId) {
        return Map.of("messages", chatService.hydrateMessages(principal.getId(), threadId), "activeRun", Map.of());
    }

    @PostMapping
    public SseEmitter chat(@AuthenticationPrincipal UserPrincipal principal, @RequestBody JsonNode request) {
        String threadId = text(request, "threadId");
        String runId = text(request, "runId");
        String message = ChatRequestParser.latestUserMessage(request.path("messages"));
        if (runId == null || runId.isBlank()) {
            runId = UUID.randomUUID().toString();
        }
        return chatService.streamMessage(principal.getId(), threadId, runId, message);
    }

    private String text(JsonNode request, String field) {
        JsonNode value = request.path(field);
        return value.isTextual() ? value.asText() : null;
    }
}