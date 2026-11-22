package com.howie.pharmacy.pharmacy_store.services;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;

import com.howie.pharmacy.pharmacy_store.dto.category.CategoryDto;
import com.howie.pharmacy.pharmacy_store.dto.category.CategoryResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.category.CategoryCreateDto;

@Component
public interface CategoryService {
    Page<CategoryResponseDto> getAllCategories(ListRequest request);

    Optional<CategoryResponseDto> create(CategoryCreateDto categoryCreateDto);

    Optional<CategoryResponseDto> update(Integer id, CategoryCreateDto categoryCreateDto);

    void delete(Integer id);

    Optional<CategoryDto> findById(Integer id);
}
