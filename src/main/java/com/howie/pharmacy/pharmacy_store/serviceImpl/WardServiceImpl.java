package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.WardImportRequest;
import com.howie.pharmacy.pharmacy_store.entity.Province;
import com.howie.pharmacy.pharmacy_store.entity.Ward;
import com.howie.pharmacy.pharmacy_store.repository.ProvinceRepository;
import com.howie.pharmacy.pharmacy_store.repository.WardRepository;
import com.howie.pharmacy.pharmacy_store.services.WardService;

@Service
public class WardServiceImpl implements WardService {

    private final WardRepository wardRepository;
    private final ProvinceRepository provinceRepository;

    public WardServiceImpl(WardRepository wardRepository, ProvinceRepository provinceRepository) {
        this.wardRepository = wardRepository;
        this.provinceRepository = provinceRepository;
    }

    @Override
    public List<Ward> getWardsByProvinceCode(String provinceCode) {
        return wardRepository.findByProvinceCode(provinceCode);
    }

    @Override
    @Transactional
    public List<Ward> importWards(String provinceCode, List<WardImportRequest> requests) {
        Province province = provinceRepository.findByProvinceCode(provinceCode)
                .orElseThrow(() -> new IllegalArgumentException("Province not found with code: " + provinceCode));

        List<Ward> savedWards = new ArrayList<>();
        for (WardImportRequest request : requests) {
            Ward ward = wardRepository.findByWardCode(request.getWardCode())
                    .orElseGet(Ward::new);

            ward.setWardCode(request.getWardCode());
            ward.setProvinceCode(province.getProvinceCode());
            ward.setProvince(province);
            ward.setName(request.getName());
            ward.setLocationSlug(request.getLocationSlug());
            ward.setLegacyAddress(request.getLegacyAddress());

            savedWards.add(wardRepository.save(ward));
        }

        return savedWards;
    }
}
