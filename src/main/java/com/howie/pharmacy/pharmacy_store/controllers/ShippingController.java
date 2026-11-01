package com.howie.pharmacy.pharmacy_store.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.howie.pharmacy.pharmacy_store.dto.shipping.ShippingQuoteDto;
import com.howie.pharmacy.pharmacy_store.dto.shipping.ShippingQuoteRequestDto;
import com.howie.pharmacy.pharmacy_store.services.GeocodingService;
import com.howie.pharmacy.pharmacy_store.services.ShippingQuoteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
    private final ShippingQuoteService shippingQuoteService;
    private final GeocodingService geocodingService;

    public ShippingController(ShippingQuoteService shippingQuoteService, GeocodingService geocodingService) {
        this.shippingQuoteService = shippingQuoteService;
        this.geocodingService = geocodingService;
    }

    @PostMapping("/quote")
    public ShippingQuoteDto quote(@Valid @RequestBody ShippingQuoteRequestDto request, HttpServletRequest http) {
        // Mỗi báo giá có thể gọi OSRM bên ngoài nên dùng chung giới hạn theo IP với
        // /api/geo
        geocodingService.checkClientLimit(http.getRemoteAddr());
        return shippingQuoteService.toDto(shippingQuoteService.calculateForAddress(request.latitude(),
                request.longitude(), request.provinceCode(), request.wardCode(), request.address(),
                request.items()));
    }
}
