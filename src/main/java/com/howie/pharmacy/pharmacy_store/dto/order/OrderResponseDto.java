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
public class OrderResponseDto {
    private Integer id;
    private Order.Payment paymentMethod;
    private Order.Status status;
    private BigDecimal totalPrice;
    private Boolean isPaid;
    private String orderCode;
    private BigDecimal shippingFee;
    private BigDecimal distanceKm;
    private String storeName;

    private Integer userId;
    // private String userName;

    private ShippingAddressDto shippingAddress;

    private List<OrderItemDto> orderItems;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
