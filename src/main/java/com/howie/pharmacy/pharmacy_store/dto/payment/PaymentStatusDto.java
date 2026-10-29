package com.howie.pharmacy.pharmacy_store.dto.payment;

import java.math.BigDecimal;

import com.howie.pharmacy.pharmacy_store.entity.Order;

public record PaymentStatusDto(
        Integer orderId,
        String orderCode,
        Long payosOrderCode,
        // PENDING | PAID | CANCELLED
        String paymentStatus,
        Order.Status orderStatus,
        BigDecimal amount) {
}
