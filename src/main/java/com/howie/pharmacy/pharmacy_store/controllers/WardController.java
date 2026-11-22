package com.howie.pharmacy.pharmacy_store.controllers;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.howie.pharmacy.pharmacy_store.dto.WardSearchRequest;
import com.howie.pharmacy.pharmacy_store.entity.Ward;
import com.howie.pharmacy.pharmacy_store.services.WardService;

@RestController
@RequestMapping("/api/wards")
public class WardController {
    private final WardService wardService;

    public WardController(WardService wardService) {
        this.wardService = wardService;
    }

    @PostMapping("/getAllWards")
    public Page<Ward> getAllWards(@RequestBody WardSearchRequest request) {
        return wardService.getAllWards(request);
    }
}
