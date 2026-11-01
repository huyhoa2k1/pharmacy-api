package com.howie.pharmacy.pharmacy_store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.howie.pharmacy.pharmacy_store.entity.Store;

public interface StoreRepository extends JpaRepository<Store, Integer> {
    List<Store> findAllByOrderByNameAsc();

    List<Store> findByIsActiveTrueOrderByNameAsc();

    List<Store> findByProvinceCodeAndIsActiveTrueOrderByNameAsc(String provinceCode);

    List<Store> findByProvinceCodeOrderByNameAsc(String provinceCode);

    Optional<Store> findFirstByIsActiveTrueOrderByIdAsc();
}
