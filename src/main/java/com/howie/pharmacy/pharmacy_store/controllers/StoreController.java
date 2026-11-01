package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.howie.pharmacy.pharmacy_store.dto.store.StoreDto;
import com.howie.pharmacy.pharmacy_store.dto.store.StoreInventoryDto;
import com.howie.pharmacy.pharmacy_store.dto.store.StoreInventoryUpdateDto;
import com.howie.pharmacy.pharmacy_store.dto.store.StoreRequestDto;
import com.howie.pharmacy.pharmacy_store.services.StoreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/stores")
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<StoreDto> getStores(@RequestParam(required = false) String provinceCode,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return storeService.list(provinceCode, includeInactive);
    }

    @GetMapping("/{id}")
    public StoreDto getStore(@PathVariable Integer id) {
        return storeService.get(id);
    }

    @PostMapping
    public ResponseEntity<StoreDto> createStore(@Valid @RequestBody StoreRequestDto request) {
        return new ResponseEntity<>(storeService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public StoreDto updateStore(@PathVariable Integer id, @Valid @RequestBody StoreRequestDto request) {
        return storeService.update(id, request);
    }

    @GetMapping("/{id}/inventory")
    public List<StoreInventoryDto> getInventory(@PathVariable Integer id) {
        return storeService.getInventory(id);
    }

    @PutMapping("/{id}/inventory")
    public StoreInventoryDto setInventory(@PathVariable Integer id,
            @Valid @RequestBody StoreInventoryUpdateDto request) {
        return storeService.setInventory(id, request);
    }
}
