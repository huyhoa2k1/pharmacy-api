package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.RateLimitExceededException;

/**
 * Rate limit cho API login/register dùng Redis:
 * - Login theo IP: tối đa 10 request / 1 phút.
 * - Login theo tài khoản: khóa tạm thời sau 5 lần đăng nhập sai / 10 phút.
 * - Register theo IP: tối đa 5 request / 1 giờ.
 */
@Service
public class LoginRateLimiterService {

    private static final String IP_KEY_PREFIX = "rate-limit:login:ip:";
    private static final int IP_LIMIT = 10;
    private static final Duration IP_WINDOW = Duration.ofMinutes(1);

    private static final String ACCOUNT_KEY_PREFIX = "rate-limit:login:account:";
    private static final int ACCOUNT_FAILED_LIMIT = 5;
    private static final Duration ACCOUNT_WINDOW = Duration.ofMinutes(10);

    private static final String REGISTER_IP_KEY_PREFIX = "rate-limit:register:ip:";
    private static final int REGISTER_IP_LIMIT = 5;
    private static final Duration REGISTER_IP_WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkIpLimit(String ip) {
        long count = incrementWithExpiry(IP_KEY_PREFIX + ip, IP_WINDOW);
        if (count > IP_LIMIT) {
            throw new RateLimitExceededException("Too many login requests, please try again later");
        }
    }

    public void checkAccountLocked(String phone) {
        String value = redisTemplate.opsForValue().get(ACCOUNT_KEY_PREFIX + phone);
        long failedCount = value == null ? 0 : Long.parseLong(value);
        if (failedCount >= ACCOUNT_FAILED_LIMIT) {
            throw new RateLimitExceededException(
                    "Account is temporarily locked due to too many failed login attempts, please try again later");
        }
    }

    public void recordFailedAttempt(String phone) {
        incrementWithExpiry(ACCOUNT_KEY_PREFIX + phone, ACCOUNT_WINDOW);
    }

    public void resetFailedAttempts(String phone) {
        redisTemplate.delete(ACCOUNT_KEY_PREFIX + phone);
    }

    public void checkRegisterIpLimit(String ip) {
        long count = incrementWithExpiry(REGISTER_IP_KEY_PREFIX + ip, REGISTER_IP_WINDOW);
        if (count > REGISTER_IP_LIMIT) {
            throw new RateLimitExceededException("Too many registration requests, please try again later");
        }
    }

    // INCR nguyên tử, chỉ set TTL ở lần tạo key đầu tiên
    private long incrementWithExpiry(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count == null ? 0 : count;
    }
}
