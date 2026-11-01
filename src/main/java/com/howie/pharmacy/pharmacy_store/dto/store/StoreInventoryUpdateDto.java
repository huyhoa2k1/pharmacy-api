package com.howie.pharmacy.pharmacy_store.dto.store;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StoreInventoryUpdateDto(@NotNull Integer productId, @NotNull @Min(0) Integer amount) {
}
