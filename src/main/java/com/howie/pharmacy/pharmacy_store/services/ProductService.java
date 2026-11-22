package com.howie.pharmacy.pharmacy_store.services;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductSearchRequest;

@Component
public interface ProductService {
        Page<ProductResponseDto> getAllProducts(ProductSearchRequest request);

        Optional<ProductResponseDto> create(ProductCreateDto productCreateDto);

        Optional<ProductResponseDto> update(Integer id, ProductCreateDto productCreateDto);

        void delete(Integer id);

        Optional<ProductDto> findById(Integer id);

        List<ProductResponseDto> findByIds(List<Integer> ids);

        List<ProductResponseDto> findByPriceRange(BigDecimal minimum, BigDecimal maximum, int limit);

        List<ProductResponseDto> searchProducts(String keyword);

        boolean setSale(List<Integer> productIds, Boolean isSale, Float discount,
                        java.time.LocalDateTime saleEndTime);
}
