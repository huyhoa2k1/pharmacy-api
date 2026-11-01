package com.howie.pharmacy.pharmacy_store.services;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howie.pharmacy.pharmacy_store.dto.geo.GeoPlaceDto;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.RateLimitExceededException;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.ServiceUnavailableException;

/**
 * Proxy tới Nominatim (OpenStreetMap): cache Redis, giới hạn theo IP và tối đa
 * 1 request/giây tới Nominatim.
 */
@Service
public class GeocodingService {

    private static final String CACHE_PREFIX = "geo:nominatim:";
    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final String CLIENT_LIMIT_PREFIX = "rate-limit:geo:ip:";
    private static final int CLIENT_LIMIT = 30;
    private static final Duration CLIENT_WINDOW = Duration.ofMinutes(1);
    private static final long MIN_INTERVAL_NANOS = Duration.ofMillis(1100).toNanos();
    private static final int MAX_QUERY_LENGTH = 200;

    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String countryCodes;
    private final ReentrantLock throttleLock = new ReentrantLock();
    private long lastCallNanos = System.nanoTime() - MIN_INTERVAL_NANOS;

    public GeocodingService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
            @Value("${pharmacy.geo.nominatim-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${pharmacy.geo.user-agent:PharmacyStore/1.0}") String userAgent,
            @Value("${pharmacy.geo.country-codes:vn}") String countryCodes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.countryCodes = countryCodes;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Language", "vi")
                .build();
    }

    public void checkClientLimit(String clientIp) {
        String key = CLIENT_LIMIT_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, CLIENT_WINDOW);
        }
        if (count != null && count > CLIENT_LIMIT) {
            throw new RateLimitExceededException("Too many location requests, please try again later");
        }
    }

    public List<GeoPlaceDto> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 3) {
            return List.of();
        }
        if (q.length() > MAX_QUERY_LENGTH) {
            q = q.substring(0, MAX_QUERY_LENGTH);
        }
        final String finalQuery = q;
        return cached(
                "search:"
                        + UUID.nameUUIDFromBytes(finalQuery.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)),
                () -> {
                    JsonNode body = call("/search?q={q}&format=jsonv2&limit=5&countrycodes={cc}",
                            Map.of("q", finalQuery, "cc", countryCodes));
                    List<GeoPlaceDto> places = new ArrayList<>();
                    if (body != null && body.isArray()) {
                        body.forEach(node -> toPlace(node).ifPresent(places::add));
                    }
                    return places;
                });
    }

    public Optional<GeoPlaceDto> reverse(double latitude, double longitude) {
        String key = String.format(Locale.ROOT, "reverse:%.5f,%.5f", latitude, longitude);
        List<GeoPlaceDto> result = cached(key, () -> {
            JsonNode body = call("/reverse?lat={lat}&lon={lon}&format=jsonv2",
                    Map.of("lat", latitude, "lon", longitude));
            return body == null ? List.<GeoPlaceDto>of() : toPlace(body).map(List::of).orElse(List.of());
        });
        return result.stream().findFirst();
    }

    private JsonNode call(String uriTemplate, Map<String, ?> variables) {
        throttle();
        try {
            return restClient.get().uri(uriTemplate, variables).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Location service is temporarily unavailable");
        }
    }

    // Chính sách Nominatim: tối đa 1 request/giây
    private void throttle() {
        throttleLock.lock();
        try {
            long waitNanos = MIN_INTERVAL_NANOS - (System.nanoTime() - lastCallNanos);
            if (waitNanos > 0) {
                Thread.sleep(Duration.ofNanos(waitNanos).toMillis() + 1);
            }
            lastCallNanos = System.nanoTime();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Location service is temporarily unavailable");
        } finally {
            throttleLock.unlock();
        }
    }

    private Optional<GeoPlaceDto> toPlace(JsonNode node) {
        JsonNode lat = node.get("lat");
        JsonNode lon = node.get("lon");
        JsonNode name = node.get("display_name");
        if (lat == null || lon == null || name == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new GeoPlaceDto(name.asText(), Double.parseDouble(lat.asText()),
                    Double.parseDouble(lon.asText())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private List<GeoPlaceDto> cached(String key, java.util.function.Supplier<List<GeoPlaceDto>> loader) {
        String cacheKey = CACHE_PREFIX + key;
        String hit = redisTemplate.opsForValue().get(cacheKey);
        if (hit != null) {
            try {
                return objectMapper.readValue(hit, new TypeReference<List<GeoPlaceDto>>() {
                });
            } catch (JsonProcessingException e) {
                // cache hỏng: bỏ qua và gọi lại Nominatim
            }
        }
        List<GeoPlaceDto> loaded = loader.get();
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(loaded), CACHE_TTL);
        } catch (JsonProcessingException e) {
            // không cache được vẫn trả kết quả
        }
        return loaded;
    }
}
