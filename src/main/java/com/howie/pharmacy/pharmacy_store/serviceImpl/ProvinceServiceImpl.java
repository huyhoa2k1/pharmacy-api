package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.ProvinceImportRequest;
import com.howie.pharmacy.pharmacy_store.entity.Province;
import com.howie.pharmacy.pharmacy_store.repository.ProvinceRepository;
import com.howie.pharmacy.pharmacy_store.services.ProvinceService;

@Service
public class ProvinceServiceImpl implements ProvinceService {
    @Autowired
    private ProvinceRepository provinceRepository;

    @Override
    public List<Province> findAll() {
        List<Province> provinces = provinceRepository.findAllByOrderByNameAsc();
        return provinces;
    }

    @Override
    public Optional<Province> findByProvinceCode(String provinceCode) {
        return provinceRepository.findByProvinceCode(provinceCode);
    }

    @Override
    @Transactional
    public List<Province> importProvinces(List<ProvinceImportRequest> requests) {
        List<Province> savedProvinces = new ArrayList<>();

        for (ProvinceImportRequest request : requests) {
            Province province = provinceRepository.findByProvinceCode(request.getProvinceCode())
                    .orElseGet(Province::new);

            province.setProvinceCode(request.getProvinceCode());
            province.setName(request.getName());
            province.setLocationSlug(request.getLocationSlug());
            province.setLegacyAddress(request.getLegacyAddress());

            Province savedProvince = provinceRepository.save(province);
            savedProvinces.add(savedProvince);
        }

        return savedProvinces;
    }
}
