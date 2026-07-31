package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.howie.pharmacy.pharmacy_store.dto.brand.BrandDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandCreateDto;
import com.howie.pharmacy.pharmacy_store.entity.Brand;
import com.howie.pharmacy.pharmacy_store.entity.Category;
import com.howie.pharmacy.pharmacy_store.mapper.BrandMapper;
import com.howie.pharmacy.pharmacy_store.repository.BrandRepository;
import com.howie.pharmacy.pharmacy_store.repository.CategoryRepository;
import com.howie.pharmacy.pharmacy_store.services.BrandService;

import jakarta.transaction.Transactional;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final BrandMapper brandMapper;

    public BrandServiceImpl(BrandRepository brandRepository, CategoryRepository categoryRepository,
            BrandMapper brandMapper) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.brandMapper = brandMapper;
    }

    @Transactional
    @Override
    public List<BrandResponseDto> findAll() {
        List<Brand> brands = brandRepository.findAll();
        return brandMapper.toResponseDtoList(brands);
    }

    @Override
    public Optional<BrandResponseDto> create(BrandCreateDto brandCreateDtoo) {
        if (brandRepository.existsByName(brandCreateDtoo.getName())) {
            throw new IllegalArgumentException("Brand with this name already exists");
        }

        Category category = categoryRepository.findById(brandCreateDtoo.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Brand brand = brandMapper.toEntity(brandCreateDtoo);
        brand.setCategory(category);
        brand.setCreatedAt(java.time.LocalDateTime.now());
        brand.setUpdatedAt(java.time.LocalDateTime.now());

        return Optional.of(brandMapper.toResponseDto(brandRepository.save(brand)));
    }

    @Override
    public Optional<BrandResponseDto> update(Integer id, BrandCreateDto brandCreateDto) {
        Brand existingBrand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        if (brandRepository.existsByName(brandCreateDto.getName())
                && !existingBrand.getName().equals(brandCreateDto.getName())) {
            throw new IllegalArgumentException("Brand with this name already exists");
        }

        Category category = categoryRepository.findById(brandCreateDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        brandMapper.updateEntityFromDto(brandCreateDto, existingBrand);
        existingBrand.setCategory(category);
        existingBrand.setUpdatedAt(java.time.LocalDateTime.now());

        return Optional.of(brandMapper.toResponseDto(brandRepository.save(existingBrand)));
    }

    @Override
    public void delete(Integer id) {
        if (!brandRepository.existsById(id)) {
            throw new IllegalArgumentException("Brand not found");
        }
        brandRepository.deleteById(id);
    }

    @Override
    public Optional<BrandDto> findById(Integer id) {
        return brandRepository.findById(id)
                .map(brandMapper::toDto);
    }

    @Override
    @Transactional
    public List<BrandResponseDto> getBrandsByCategory(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        List<Brand> brands = brandRepository.findByCategoryIdWithCategory(categoryId);
        return brandMapper.toResponseDtoList(brands);
    }

}
