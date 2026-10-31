package com.howie.pharmacy.pharmacy_store.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.BeanMapping;
import org.mapstruct.factory.Mappers;

import com.howie.pharmacy.pharmacy_store.dto.category.CategoryDto;
import com.howie.pharmacy.pharmacy_store.dto.category.CategoryResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.category.CategoryCreateDto;
import com.howie.pharmacy.pharmacy_store.entity.Category;

import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { BrandMapper.class })
public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    CategoryDto toDto(Category category);

    CategoryResponseDto toResponseDto(Category category);

    List<CategoryResponseDto> toResponseDtoList(List<Category> categories);

    @Mapping(target = "brands", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryCreateDto categoryCreateDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "brands", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CategoryCreateDto categoryCreateDto, @MappingTarget Category category);
}
