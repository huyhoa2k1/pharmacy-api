package com.howie.pharmacy.pharmacy_store.services;

import java.util.List;

import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.order.OrderCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderResponseDto;
import com.howie.pharmacy.pharmacy_store.entity.Order;

@Component
public interface OrderService {
    List<OrderResponseDto> getAllOrders();

    List<OrderResponseDto> getAllOrdersByUserId(Integer userId);

    OrderDto getOrderById(Integer id);

    OrderDto createOrder(OrderCreateDto orderCreateDto);

    OrderDto updateOrder(Integer id, OrderCreateDto orderCreateDto);

    void deleteOrder(Integer id);

    OrderDto updateOrderStatus(Integer id, Order.Status newStatus);
}
