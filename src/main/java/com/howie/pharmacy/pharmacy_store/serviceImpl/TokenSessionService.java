package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenSessionService {
    private static final String KEY_PREFIX = "auth:revoked-session:";
    private static final Duration REVOCATION_TTL = Duration.ofDays(3).plusMinutes(5);
    private final StringRedisTemplate redisTemplate;

    public TokenSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String sessionId(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public void revoke(String sessionId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, "1", REVOCATION_TTL);
    }

    public boolean isRevoked(String sessionId) {
        Boolean revoked = redisTemplate.hasKey(KEY_PREFIX + sessionId);
        if (revoked == null) {
            throw new IllegalStateException("Unable to check session revocation");
        }
        return revoked;
    }
}