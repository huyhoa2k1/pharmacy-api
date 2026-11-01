package com.howie.pharmacy.pharmacy_store.dto.store;

public record StoreDto(
                Integer id,
                String name,
                String provinceCode,
                String address,
                String phone,
                Double latitude,
                Double longitude,
                Boolean isActive) {
}
