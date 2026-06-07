package com.howie.pharmacy.pharmacy_store.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;

@Component
public interface ProductService {
    List<ProductResponseDto> findAll();

    Page<ProductResponseDto> getAllProductsByCategory(Integer categoryId, List<Integer> brandIds, Double minPrice,
            Double maxPrice, String sortBy, String sortDirection, int pageNo,
            int pageSize);

    Optional<ProductResponseDto> create(ProductCreateDto productCreateDto);

    Optional<ProductResponseDto> update(Integer id, ProductCreateDto productCreateDto);

    void delete(Integer id);

    Optional<ProductDto> findById(Integer id);

    List<ProductResponseDto> searchProducts(String keyword);

    boolean setSale(List<Integer> productIds, Boolean isSale, Float discount,
            java.time.LocalDateTime saleEndTime);
}
