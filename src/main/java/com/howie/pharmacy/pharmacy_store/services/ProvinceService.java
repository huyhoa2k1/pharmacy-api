package com.howie.pharmacy.pharmacy_store.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.ProvinceImportRequest;
import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;
import com.howie.pharmacy.pharmacy_store.entity.Province;

@Component
public interface ProvinceService {
    Page<Province> getAllProvinces(ListRequest request);

    Optional<Province> findByProvinceCode(String provinceCode);

    List<Province> importProvinces(List<ProvinceImportRequest> requests);
}
