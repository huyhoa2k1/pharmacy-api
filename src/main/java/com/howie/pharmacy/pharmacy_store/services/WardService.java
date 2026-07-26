package com.howie.pharmacy.pharmacy_store.services;

import java.util.List;

import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.WardImportRequest;
import com.howie.pharmacy.pharmacy_store.entity.Ward;

@Component
public interface WardService {
    List<Ward> getWardsByProvinceCode(String provinceCode);

    List<Ward> importWards(String provinceCode, List<WardImportRequest> requests);
}
