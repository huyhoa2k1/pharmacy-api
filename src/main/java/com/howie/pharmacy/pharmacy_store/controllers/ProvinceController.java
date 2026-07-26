package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.howie.pharmacy.pharmacy_store.dto.ProvinceImportRequest;
import com.howie.pharmacy.pharmacy_store.dto.WardImportRequest;
import com.howie.pharmacy.pharmacy_store.entity.Province;
import com.howie.pharmacy.pharmacy_store.services.ProvinceService;
import com.howie.pharmacy.pharmacy_store.services.WardService;

@RestController
@RequestMapping("/api/provinces")
public class ProvinceController {
    @Autowired
    private ProvinceService provinceService;

    @Autowired
    private WardService wardService;

    @GetMapping()
    public List<Province> getAllProvinces() {
        return provinceService.findAll();
    }

    @PostMapping("/import")
    public ResponseEntity<Boolean> importProvinces(@Valid @RequestBody List<ProvinceImportRequest> requests) {
        provinceService.importProvinces(requests);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/{provinceCode}/wards/import")
    public ResponseEntity<Boolean> importWardsForProvince(
            @PathVariable String provinceCode,
            @Valid @RequestBody List<WardImportRequest> requests) {
        wardService.importWards(provinceCode, requests);
        return ResponseEntity.ok(true);
    }
}
