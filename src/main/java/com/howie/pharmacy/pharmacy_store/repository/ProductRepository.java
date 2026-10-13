package com.howie.pharmacy.pharmacy_store.repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.howie.pharmacy.pharmacy_store.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
        boolean existsByName(String name);

        Product findByName(String name);

        @EntityGraph(attributePaths = { "brand", "brand.category" })
        @Query("SELECT p FROM Product p WHERE p.id = :id")
        Optional<Product> findDetailedById(@Param("id") Integer id);

        @Query("SELECT p.id FROM Product p")
        List<Integer> findAllIds(Pageable pageable);

        @Query("SELECT p.id FROM Product p WHERE p.brand.id = :brandId")
        List<Integer> findIdsByBrandId(@Param("brandId") Integer brandId);

        @Query("SELECT p.id FROM Product p WHERE p.brand.category.id = :categoryId")
        List<Integer> findIdsByCategoryId(@Param("categoryId") Integer categoryId);

        @Query(value = """
                        SELECT p.*
                        FROM products p
                        WHERE (:minPrice IS NULL OR
                               CAST(CASE WHEN p.is_sale = TRUE
                                             AND p.sale_end_time > CURRENT_TIMESTAMP
                                             AND p.discount > 0 AND p.discount <= 100
                                        THEN p.price * (100.0 - p.discount) / 100.0
                                        ELSE p.price
                                   END AS numeric) >= :minPrice)
                          AND (:maxPrice IS NULL OR
                               CAST(CASE WHEN p.is_sale = TRUE
                                             AND p.sale_end_time > CURRENT_TIMESTAMP
                                             AND p.discount > 0 AND p.discount <= 100
                                        THEN p.price * (100.0 - p.discount) / 100.0
                                        ELSE p.price
                                   END AS numeric) <= :maxPrice)
                        ORDER BY p.sold DESC NULLS LAST, p.price ASC
                        """, nativeQuery = true)
        List<Product> findProductsByPriceRange(@Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

        @Query("SELECT p FROM Product p JOIN p.brand b JOIN b.category c" + " WHERE c.id = :categoryId"
                        + " AND (:brandIds IS NULL OR b.id IN :brandIds)"
                        + " AND (:minPrice IS NULL OR p.price >= :minPrice)"
                        + " AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
        Page<Product> getAllProductsByCategory(Integer categoryId, @Param("brandIds") List<Integer> brandIds,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        Pageable pageable);

        @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))"
                        + " OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Product> searchProducts(@Param("keyword") String keyword);

        @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p WHERE p.isSale = true AND p.saleEndTime < :currentTime")
        boolean existsExpiredSales(@Param("currentTime") LocalDateTime currentTime);

        @Modifying
        @Query("UPDATE Product p SET p.isSale = false WHERE p.isSale = true AND p.saleEndTime < :currentTime")
        int closeExpiredSales(@Param("currentTime") LocalDateTime currentTime);

        @Query("SELECT p FROM Product p WHERE p.isSale = true")
        List<Product> getProductsOnSale();

        @Query("SELECT p FROM Product p WHERE p.sold >= :minSold ORDER BY p.sold DESC")
        List<Product> getBestSellingProducts(@Param("minSold") Integer minSold);
}
