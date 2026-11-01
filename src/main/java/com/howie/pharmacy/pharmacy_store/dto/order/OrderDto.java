package com.howie.pharmacy.pharmacy_store.dto.order;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.howie.pharmacy.pharmacy_store.dto.shippingaddress.ShippingAddressDto;
import com.howie.pharmacy.pharmacy_store.entity.Order;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Integer id;
    private Order.Payment paymentMethod;
    private Boolean isPaid;
    private BigDecimal totalPrice;
    private Order.Status status;
    private String slugStatus;
    private String orderCode;
    private BigDecimal shippingFee;
    private BigDecimal distanceKm;
    private String storeName;

    // private UserDto user; // User đầy đủ thông tin
    private ShippingAddressDto shippingAddress; // ShippingAddress đầy đủ thông tin

    private List<OrderItemDto> orderItems; // Danh sách OrderItem đầy đủ thông tin

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
