package com.howie.pharmacy.pharmacy_store.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.howie.pharmacy.pharmacy_store.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
        boolean existsByName(String name);

        Product findByName(String name);

        @Query("SELECT p FROM Product p JOIN p.brand b JOIN b.category c" + " WHERE c.id = :categoryId"
                        + " AND (:brandIds IS NULL OR b.id IN :brandIds)"
                        + " AND (:minPrice IS NULL OR p.price >= :minPrice)"
                        + " AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
        Page<Product> getAllProductsByCategory(Integer categoryId, @Param("brandIds") List<Integer> brandIds,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        Pageable pageable);

        @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Product> searchProducts(@Param("keyword") String keyword);

        @Modifying
        @Query("UPDATE Product p SET p.isSale = false WHERE p.isSale = true AND p.saleEndTime < :currentTime")
        int closeExpiredSales(@Param("currentTime") LocalDateTime currentTime);

        @Query("SELECT p FROM Product p WHERE p.isSale = true")
        List<Product> getProductsOnSale();
}
