package com.howie.pharmacy.pharmacy_store.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.ProvinceImportRequest;
import com.howie.pharmacy.pharmacy_store.entity.Province;

@Component
public interface ProvinceService {
    List<Province> findAll();

    Optional<Province> findByProvinceCode(String provinceCode);

    List<Province> importProvinces(List<ProvinceImportRequest> requests);
}
