package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.ProvinceImportRequest;
import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;
import com.howie.pharmacy.pharmacy_store.entity.Province;
import com.howie.pharmacy.pharmacy_store.repository.ProvinceRepository;
import com.howie.pharmacy.pharmacy_store.services.ProvinceService;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder.Field;

@Service
public class ProvinceServiceImpl implements ProvinceService {
    private final ProvinceRepository provinceRepository;

    public ProvinceServiceImpl(ProvinceRepository provinceRepository) {
        this.provinceRepository = provinceRepository;
    }

    private static final Map<String, Field> PROVINCE_FIELDS = Map.of(
            "id", Field.of("id"),
            "provinceCode", Field.of("provinceCode"),
            "name", Field.of("name"),
            "locationSlug", Field.of("locationSlug"));

    @Override
    public Page<Province> getAllProvinces(ListRequest request) {
        return SpecificationBuilder.page(provinceRepository, request.getPager(), PROVINCE_FIELDS,
                Sort.by(Sort.Direction.ASC, "name"), null);
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
