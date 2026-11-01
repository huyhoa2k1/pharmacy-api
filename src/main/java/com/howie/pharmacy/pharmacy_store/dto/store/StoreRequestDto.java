package com.howie.pharmacy.pharmacy_store.dto.store;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoreRequestDto(
                @NotBlank @Size(max = 255) String name,
                @Size(max = 255) String provinceCode,
                @NotBlank String address,
                @Size(max = 50) String phone,
                @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
                @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
                Boolean isActive) {
}
