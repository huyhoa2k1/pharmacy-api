package com.howie.pharmacy.pharmacy_store.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.howie.pharmacy.pharmacy_store.serviceImpl.TokenSessionService;

import static org.mockito.Mockito.mock;

class TokenSessionServiceTest {
    @Test
    void hashesRefreshTokenAndStoresRevocationWithExpiry() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        TokenSessionService service = new TokenSessionService(redis);

        String sessionId = service.sessionId("refresh-token");
        assertEquals(sessionId, service.sessionId("refresh-token"));
        assertNotEquals("refresh-token", sessionId);
        service.revoke(sessionId);
        verify(values).set("auth:revoked-session:" + sessionId, "1", Duration.ofDays(3).plusMinutes(5));
    }

    @Test
    void checksRevocationAndFailsClosedOnUnknownResult() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        TokenSessionService service = new TokenSessionService(redis);
        when(redis.hasKey("auth:revoked-session:session")).thenReturn(false, true, null);

        assertFalse(service.isRevoked("session"));
        assertTrue(service.isRevoked("session"));
        assertThrows(IllegalStateException.class, () -> service.isRevoked("session"));
    }
}