package com.howie.pharmacy.pharmacy_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.howie.pharmacy.pharmacy_store.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer>, JpaSpecificationExecutor<Category> {
    Category findBySlug(String slug);

    boolean existsByName(String name);
}
