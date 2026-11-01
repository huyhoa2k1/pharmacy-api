package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.howie.pharmacy.pharmacy_store.dto.geo.GeoPlaceDto;
import com.howie.pharmacy.pharmacy_store.services.GeocodingService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/geo")
public class GeoController {
    private final GeocodingService geocodingService;

    public GeoController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping("/search")
    public List<GeoPlaceDto> search(@RequestParam String q, HttpServletRequest request) {
        geocodingService.checkClientLimit(request.getRemoteAddr());
        return geocodingService.search(q);
    }

    @GetMapping("/reverse")
    public ResponseEntity<GeoPlaceDto> reverse(@RequestParam double lat, @RequestParam double lng,
            HttpServletRequest request) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return ResponseEntity.badRequest().build();
        }
        geocodingService.checkClientLimit(request.getRemoteAddr());
        return geocodingService.reverse(lat, lng)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
