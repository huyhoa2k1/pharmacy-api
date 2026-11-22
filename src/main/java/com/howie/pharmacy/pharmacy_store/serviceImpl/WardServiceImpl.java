package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.WardImportRequest;
import com.howie.pharmacy.pharmacy_store.dto.WardSearchRequest;
import com.howie.pharmacy.pharmacy_store.entity.Province;
import com.howie.pharmacy.pharmacy_store.entity.Ward;
import com.howie.pharmacy.pharmacy_store.repository.ProvinceRepository;
import com.howie.pharmacy.pharmacy_store.repository.WardRepository;
import com.howie.pharmacy.pharmacy_store.services.WardService;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder.Field;

@Service
public class WardServiceImpl implements WardService {

    private final WardRepository wardRepository;
    private final ProvinceRepository provinceRepository;

    public WardServiceImpl(WardRepository wardRepository, ProvinceRepository provinceRepository) {
        this.wardRepository = wardRepository;
        this.provinceRepository = provinceRepository;
    }

    private static final Map<String, Field> WARD_FIELDS = Map.of(
            "id", Field.of("id"),
            "wardCode", Field.of("wardCode"),
            "provinceCode", Field.of("provinceCode"),
            "name", Field.of("name"),
            "locationSlug", Field.of("locationSlug"));

    @Override
    public Page<Ward> getAllWards(WardSearchRequest request) {
        Specification<Ward> extra = null;
        if (request.getProvinceCode() != null && !request.getProvinceCode().isBlank()) {
            String provinceCode = request.getProvinceCode();
            extra = (root, q, cb) -> cb.equal(root.get("provinceCode"), provinceCode);
        }
        return SpecificationBuilder.page(wardRepository, request.getPager(), WARD_FIELDS,
                Sort.by(Sort.Direction.ASC, "id"), extra);
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
