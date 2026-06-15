package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.howie.pharmacy.pharmacy_store.dto.order.OrderCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderResponseDto;
import com.howie.pharmacy.pharmacy_store.entity.Order;
import com.howie.pharmacy.pharmacy_store.services.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<OrderResponseDto> orders = orderService.getAllOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDto>> getAllOrdersByUserId(@PathVariable Integer userId) {
        List<OrderResponseDto> orders = orderService.getAllOrdersByUserId(userId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderCreateDto orderCreateDto) {
        OrderDto newOrder = orderService.createOrder(orderCreateDto);
        return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
    }

    @PatchMapping("/status")
    public ResponseEntity<OrderDto> updateOrderStatus(@RequestBody Map<String, String> statusUpdate) {
        Integer id = Integer.valueOf(statusUpdate.get("orderId"));
        String status = statusUpdate.get("status");
        OrderDto updatedOrder = orderService.updateOrderStatus(id, Order.Status.valueOf(status.toUpperCase()));
        return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
    }
}
