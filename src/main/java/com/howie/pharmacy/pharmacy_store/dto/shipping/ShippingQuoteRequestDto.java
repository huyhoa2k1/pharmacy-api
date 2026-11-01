package com.howie.pharmacy.pharmacy_store.dto.shipping;

import java.util.List;

import com.howie.pharmacy.pharmacy_store.dto.order.OrderItemCreateDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;

// latitude/longitude do khách chọn trên bản đồ; thiếu thì server tìm vị trí từ tỉnh/phường/địa chỉ
public record ShippingQuoteRequestDto(
                @DecimalMin("-90") @DecimalMax("90") Double latitude,
                @DecimalMin("-180") @DecimalMax("180") Double longitude,
                String provinceCode,
                String wardCode,
                String address,
                @NotEmpty @Valid List<OrderItemCreateDto> items) {
}
