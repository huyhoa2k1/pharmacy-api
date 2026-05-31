package com.howie.pharmacy.pharmacy_store.mapper;

import java.util.List;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;
import com.howie.pharmacy.pharmacy_store.entity.Product;

@Mapper(componentModel = "spring", uses = { BrandMapper.class }, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(target = "brand", source = "brand")
    ProductDto toDto(Product product);

    @Mapping(target = "brand", source = "brand")
    ProductResponseDto toResponseDto(Product product);

    List<ProductResponseDto> toResponseDtoList(List<Product> products);

    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    // @Mapping(target = "slug", ignore = true)
    // @Mapping(target = "amount", ignore = true)
    // @Mapping(target = "discount", ignore = true)
    // @Mapping(target = "price", ignore = true)
    // @Mapping(target = "description", ignore = true)
    @Mapping(target = "sold", ignore = true)
    @Mapping(target = "isSale", ignore = true)
    @Mapping(target = "saleEndTime", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    Product toEntity(ProductCreateDto productCreateDto);

    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    // @Mapping(target = "slug", ignore = true)
    // @Mapping(target = "amount", ignore = true)
    // @Mapping(target = "discount", ignore = true)
    // @Mapping(target = "price", ignore = true)
    // @Mapping(target = "description", ignore = true)
    @Mapping(target = "sold", ignore = true)
    @Mapping(target = "isSale", ignore = true)
    @Mapping(target = "saleEndTime", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    void updateEntityFromDto(ProductCreateDto productCreateDto, @MappingTarget Product product);
}
