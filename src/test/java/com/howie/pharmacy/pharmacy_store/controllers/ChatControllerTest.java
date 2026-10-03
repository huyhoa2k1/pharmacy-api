package com.howie.pharmacy.pharmacy_store.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ChatControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsLatestUserTextFromTanStackMessageParts() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [
                  {"role":"user","parts":[{"type":"text","content":"older question"}]},
                  {"role":"assistant","parts":[{"type":"text","content":"answer"}]},
                  {"role":"user","parts":[{"type":"text","content":"current question"}]}
                ]
                """);

        assertEquals("current question", ChatRequestParser.latestUserMessage(messages));
    }

    @Test
    void extractsLatestUserTextFromWireContent() throws Exception {
        JsonNode messages = objectMapper.readTree("""
                [{"role":"user","content":"what is this product?"}]
                """);

        assertEquals("what is this product?", ChatRequestParser.latestUserMessage(messages));
    }

    @Test
    void rejectsPayloadWithoutUserMessage() throws Exception {
        JsonNode messages = objectMapper.readTree("[{\"role\":\"assistant\",\"content\":\"hello\"}]");

        assertThrows(ResponseStatusException.class, () -> ChatRequestParser.latestUserMessage(messages));
    }
}