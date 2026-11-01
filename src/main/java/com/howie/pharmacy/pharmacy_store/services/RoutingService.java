package com.howie.pharmacy.pharmacy_store.services;

import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tính quãng đường lái xe qua OSRM; khi OSRM lỗi thì ước tính theo đường chim
 * bay.
 */
@Service
public class RoutingService {

    public record Route(double distanceKm, int durationMinutes, boolean estimated) {
    }

    private static final String CACHE_PREFIX = "geo:osrm:";
    private static final Duration CACHE_TTL = Duration.ofHours(12);
    private static final double FALLBACK_SPEED_KMH = 30.0;
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;

    public RoutingService(StringRedisTemplate redisTemplate,
            @Value("${pharmacy.geo.osrm-url:https://router.project-osrm.org}") String baseUrl,
            @Value("${pharmacy.geo.user-agent:PharmacyStore/1.0}") String userAgent) {
        this.redisTemplate = redisTemplate;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    public Route route(double fromLat, double fromLng, double toLat, double toLng) {
        // OSRM nhận toạ độ theo thứ tự lng,lat
        String coordinates = String.format(Locale.ROOT, "%.5f,%.5f;%.5f,%.5f", fromLng, fromLat, toLng, toLat);
        String cacheKey = CACHE_PREFIX + coordinates;

        String hit = redisTemplate.opsForValue().get(cacheKey);
        if (hit != null) {
            String[] parts = hit.split("\\|");
            if (parts.length == 2) {
                try {
                    return new Route(Double.parseDouble(parts[0]), Integer.parseInt(parts[1]), false);
                } catch (NumberFormatException e) {
                    // cache hỏng: tính lại
                }
            }
        }

        try {
            JsonNode body = restClient.get()
                    .uri("/route/v1/driving/" + coordinates + "?overview=false")
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode route = body == null ? null : body.path("routes").path(0);
            if (body != null && "Ok".equals(body.path("code").asText()) && route != null
                    && route.hasNonNull("distance")) {
                double km = route.get("distance").asDouble() / 1000.0;
                int minutes = (int) Math.round(route.path("duration").asDouble() / 60.0);
                redisTemplate.opsForValue().set(cacheKey, String.format(Locale.ROOT, "%.3f|%d", km, minutes),
                        CACHE_TTL);
                return new Route(km, minutes, false);
            }
        } catch (RestClientException e) {
            // rơi xuống ước tính bên dưới
        }

        double km = haversineKm(fromLat, fromLng, toLat, toLng);
        return new Route(km, (int) Math.round(km / FALLBACK_SPEED_KMH * 60.0), true);
    }

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }
}
