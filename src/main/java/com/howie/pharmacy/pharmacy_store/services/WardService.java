package com.howie.pharmacy.pharmacy_store.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.howie.pharmacy.pharmacy_store.dto.WardImportRequest;
import com.howie.pharmacy.pharmacy_store.dto.WardSearchRequest;
import com.howie.pharmacy.pharmacy_store.entity.Ward;

@Component
public interface WardService {
    Page<Ward> getAllWards(WardSearchRequest request);

    List<Ward> importWards(String provinceCode, List<WardImportRequest> requests);
}
