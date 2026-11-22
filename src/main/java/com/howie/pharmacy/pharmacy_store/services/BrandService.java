package com.howie.pharmacy.pharmacy_store.services;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.brand.BrandSearchRequest;

import com.howie.pharmacy.pharmacy_store.dto.brand.BrandDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandCreateDto;

@Component
public interface BrandService {
    Page<BrandResponseDto> getAllBrands(BrandSearchRequest request);

    Optional<BrandResponseDto> create(BrandCreateDto brandCreateDto);

    Optional<BrandResponseDto> update(Integer id, BrandCreateDto brandCreateDto);

    void delete(Integer id);

    Optional<BrandDto> findById(Integer id);
}
