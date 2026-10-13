package com.howie.pharmacy.pharmacy_store.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;

class ProductPriceCriteriaTest {

    @Test
    void parsesVietnameseBudgetAndRangeExpressions() {
        ProductPriceCriteria maximum = ProductPriceCriteria.parse("Tìm sản phẩm dưới 100k").orElseThrow();
        assertEquals(new BigDecimal("100000"), maximum.maximum());

        ProductPriceCriteria range = ProductPriceCriteria.parse("Từ 50k đến 100.000đ").orElseThrow();
        assertEquals(new BigDecimal("50000"), range.minimum());
        assertEquals(new BigDecimal("100000"), range.maximum());
    }

    @Test
    void matchesAgainstAnActiveDiscountedPrice() {
        ProductResponseDto product = new ProductResponseDto();
        product.setPrice(new BigDecimal("120000"));
        product.setDiscount(20f);
        product.setIsSale(true);
        product.setSaleEndTime(LocalDateTime.now().plusDays(1));

        ProductPriceCriteria criteria = ProductPriceCriteria.parse("dưới 100k").orElseThrow();
        assertTrue(criteria.matches(product));
    }
}
