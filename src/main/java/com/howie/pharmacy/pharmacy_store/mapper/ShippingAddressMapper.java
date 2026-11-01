package com.howie.pharmacy.pharmacy_store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import com.howie.pharmacy.pharmacy_store.dto.shippingaddress.ShippingAddressCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.shippingaddress.ShippingAddressDto;
import com.howie.pharmacy.pharmacy_store.entity.ShippingAddress;

@Mapper(componentModel = "spring")
public interface ShippingAddressMapper {
    ShippingAddressMapper INSTANCE = Mappers.getMapper(ShippingAddressMapper.class);

    ShippingAddressDto toDto(ShippingAddress shippingAddress);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    ShippingAddress toEntity(ShippingAddressCreateDto shippingAddressCreateDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    void updateEntityFromDto(ShippingAddressCreateDto shippingAddressCreateDto,
            @MappingTarget ShippingAddress shippingAddress);
}
