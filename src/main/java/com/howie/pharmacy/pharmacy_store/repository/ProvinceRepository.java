package com.howie.pharmacy.pharmacy_store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.howie.pharmacy.pharmacy_store.entity.Province;

public interface ProvinceRepository extends JpaRepository<Province, Integer>, JpaSpecificationExecutor<Province> {
    List<Province> findAllByOrderByNameAsc();

    Optional<Province> findByProvinceCode(String provinceCode);
}
