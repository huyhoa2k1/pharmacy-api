package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.howie.pharmacy.pharmacy_store.config.RabbitMQConfig;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderResponseDto;
import com.howie.pharmacy.pharmacy_store.entity.Order;
import com.howie.pharmacy.pharmacy_store.entity.OrderItem;
import com.howie.pharmacy.pharmacy_store.entity.Product;
import com.howie.pharmacy.pharmacy_store.entity.Order.Status;
import com.howie.pharmacy.pharmacy_store.entity.ShippingAddress;
import com.howie.pharmacy.pharmacy_store.entity.User;
import com.howie.pharmacy.pharmacy_store.mapper.OrderItemMapper;
import com.howie.pharmacy.pharmacy_store.mapper.OrderMapper;
import com.howie.pharmacy.pharmacy_store.mapper.ShippingAddressMapper;
import com.howie.pharmacy.pharmacy_store.rabbitmq.event.OrderCreateEvent;
import com.howie.pharmacy.pharmacy_store.repository.OrderRepository;
import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;
import com.howie.pharmacy.pharmacy_store.repository.UserRepository;
import com.howie.pharmacy.pharmacy_store.services.OrderService;

import jakarta.transaction.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ShippingAddressMapper shippingAddressMapper;
    private final RabbitTemplate rabbitTemplate;
    private final OrderItemMapper orderItemMapper;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper,
            ShippingAddressMapper shippingAddressMapper, RabbitTemplate rabbitTemplate, OrderItemMapper orderItemMapper,
            ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.shippingAddressMapper = shippingAddressMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.orderItemMapper = orderItemMapper;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<OrderResponseDto> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orderMapper.toResponseDtoList(orders);
    }

    @Override
    public List<OrderResponseDto> getAllOrdersByUserId(Integer userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orderMapper.toResponseDtoList(orders);
    }

    @Override
    public OrderDto getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public OrderDto createOrder(OrderCreateDto orderCreateDto) {

        String currentUserPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByPhone(currentUserPhone).orElse(null);

        User existingUser = userRepository.findByPhone(orderCreateDto.getShippingAddress().getPhone()).orElse(null);

        Order order = orderMapper.toEntity(orderCreateDto);

        if (existingUser != null) {
            order.setUser(existingUser);
        } else if (currentUser != null) {
            order.setUser(currentUser);
        } else {
            throw new IllegalArgumentException("No user found for the provided phone number.");
        }

        if (orderCreateDto.getShippingAddress() != null) {
            ShippingAddress shippingAddress = shippingAddressMapper.toEntity(orderCreateDto.getShippingAddress());
            shippingAddress.setOrder(order);
            order.setShippingAddress(shippingAddress);
        } else {
            throw new IllegalArgumentException("Shipping address is required to create an order.");
        }
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        if (orderCreateDto.getOrderItems() != null && !orderCreateDto.getOrderItems().isEmpty()) {
            for (var itemDto : orderCreateDto.getOrderItems()) {
                OrderItem item = orderItemMapper.toEntity(itemDto);
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found"));
                if (product.getAmount() < item.getAmount()) {
                    throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
                }
                product.setAmount(product.getAmount() - item.getAmount());
                productRepository.save(product);
                item.setProduct(product);
                item.setOrder(order);
                BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getAmount()));
                calculatedTotal = calculatedTotal.add(itemTotal);

                order.addOrderItem(item);
            }
        } else {
            throw new IllegalArgumentException("At least one order item is required to create an order.");
        }
        order.setTotalPrice(calculatedTotal);
        if (order.getOrderCode() == null || order.getOrderCode().isEmpty()) {
            order.setOrderCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        order.setSlugStatus(order.getStatus().name().toLowerCase());
        Order savedOrder = orderRepository.save(order);

        OrderCreateEvent orderCreateEvent = new OrderCreateEvent();
        orderCreateEvent.setOrderId(savedOrder.getId());
        orderCreateEvent.setUserId(1);
        orderCreateEvent.setUserEmail("");

        // Gửi sự kiện OrderCreateEvent đến RabbitMQ
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EVENTS_EXCHANGE, RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                orderCreateEvent);
        System.out.println("Order " + savedOrder.getId() + " created and event published to RabbitMQ.");

        return orderMapper.toDto(savedOrder);
    }

    @Override
    public OrderDto updateOrder(Integer id, OrderCreateDto orderCreateDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateOrder'");
    }

    @Override
    public void deleteOrder(Integer id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteOrder'");
    }

    private void restockProducts(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            if (product != null) {
                int updatedAmount = product.getAmount() + item.getAmount();
                product.setAmount(updatedAmount);
                productRepository.save(product);
            }
        }
    }

    @Override
    public OrderDto updateOrderStatus(Integer id, Status newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != Order.Status.CANCEL && newStatus == Order.Status.CANCEL) {
            restockProducts(order);
        }
        order.setStatus(newStatus);
        order.setSlugStatus(newStatus.name().toLowerCase());
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toDto(savedOrder);
    }

}
