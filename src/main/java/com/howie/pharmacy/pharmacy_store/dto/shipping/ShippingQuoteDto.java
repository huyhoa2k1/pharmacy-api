package com.howie.pharmacy.pharmacy_store.dto.shipping;

import java.math.BigDecimal;

public record ShippingQuoteDto(
                Integer storeId,
                String storeName,
                String storeAddress,
                Double storeLatitude,
                Double storeLongitude,
                BigDecimal distanceKm,
                Integer durationMinutes,
                // true khi OSRM không khả dụng và khoảng cách là ước tính theo đường chim bay
                boolean estimated,
                BigDecimal subtotal,
                // Chi phí vận chuyển do cửa hàng trả cho đơn vị vận chuyển, khách được miễn phí
                BigDecimal shippingFee,
                // Số tiền khách phải trả (không gồm shippingFee)
                BigDecimal total) {
}
