package com.howie.pharmacy.pharmacy_store.services;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StorageKeepAliveTask {
    private static final Logger log = LoggerFactory.getLogger(StorageKeepAliveTask.class);

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final Duration redisKeyTtl;

    public StorageKeepAliveTask(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            @Value("${pharmacy.keep-alive.redis-key-ttl:48h}") Duration redisKeyTtl) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.redisKeyTtl = redisKeyTtl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void touchStorage() {
        touchDatabase();
        touchRedis();
    }

    private void touchDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("Storage keep-alive: Supabase database is reachable");
        } catch (Exception exception) {
            log.warn("Storage keep-alive: Supabase database check failed: {}", exception.getMessage());
        }
    }

    private void touchRedis() {
        try {
            redisTemplate.opsForValue().set(
                    "pharmacy:keep-alive",
                    Instant.now().toString(),
                    redisKeyTtl);
            log.info("Storage keep-alive: Redis is reachable");
        } catch (Exception exception) {
            log.warn("Storage keep-alive: Redis check failed: {}", exception.getMessage());
        }
    }
}