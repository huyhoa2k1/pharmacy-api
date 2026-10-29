package com.howie.pharmacy.pharmacy_store.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import com.howie.pharmacy.pharmacy_store.dto.order.OrderCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderDto;
import com.howie.pharmacy.pharmacy_store.dto.order.OrderResponseDto;
import com.howie.pharmacy.pharmacy_store.entity.Order;

@Mapper(componentModel = "spring", uses = { ShippingAddressMapper.class, OrderItemMapper.class })
public interface OrderMapper {
    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @Mapping(target = "shippingAddress", source = "shippingAddress")
    OrderDto toDto(Order order);

    @Mapping(target = "shippingAddress", source = "shippingAddress")
    @Mapping(target = "orderItems", source = "orderItems")
    @Mapping(target = "userId", source = "user.id")
    OrderResponseDto toResponseDto(Order order);

    List<OrderResponseDto> toResponseDtoList(List<Order> orders);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "isPaid", ignore = true)
    @Mapping(target = "slugStatus", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "payosOrderCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "user", ignore = true)
    Order toEntity(OrderCreateDto orderCreateDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "isPaid", ignore = true)
    @Mapping(target = "slugStatus", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "payosOrderCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromDto(OrderCreateDto orderCreateDto, @MappingTarget Order order);
}
