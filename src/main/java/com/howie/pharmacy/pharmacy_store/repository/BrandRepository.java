package com.howie.pharmacy.pharmacy_store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.howie.pharmacy.pharmacy_store.entity.Brand;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer>, JpaSpecificationExecutor<Brand> {
    boolean existsByName(String name);

    Brand findByName(String name);

    @Query("SELECT b FROM Brand b JOIN FETCH b.category WHERE b.id = :id")
    Optional<Brand> findByIdWithCategory(@Param("id") Integer id);

    List<Brand> findByCategoryId(Integer categoryId);

    @Query("SELECT b FROM Brand b JOIN FETCH b.category WHERE b.category.id = :categoryId")
    List<Brand> findByCategoryIdWithCategory(@Param("categoryId") Integer categoryId);
}
