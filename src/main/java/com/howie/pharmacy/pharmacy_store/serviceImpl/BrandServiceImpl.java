package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.howie.pharmacy.pharmacy_store.dto.brand.BrandDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandSearchRequest;
import com.howie.pharmacy.pharmacy_store.dto.common.PagerRequest.FilterOp;
import com.howie.pharmacy.pharmacy_store.entity.Brand;
import com.howie.pharmacy.pharmacy_store.entity.Category;
import com.howie.pharmacy.pharmacy_store.mapper.BrandMapper;
import com.howie.pharmacy.pharmacy_store.repository.BrandRepository;
import com.howie.pharmacy.pharmacy_store.repository.CategoryRepository;
import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;
import com.howie.pharmacy.pharmacy_store.services.BrandService;
import com.howie.pharmacy.pharmacy_store.services.ProductVectorIndexService;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder.Field;

import jakarta.transaction.Transactional;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final BrandMapper brandMapper;
    private final ProductRepository productRepository;
    private final ProductVectorIndexService productVectorIndexService;

    public BrandServiceImpl(BrandRepository brandRepository, CategoryRepository categoryRepository,
            BrandMapper brandMapper, ProductRepository productRepository,
            ProductVectorIndexService productVectorIndexService) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.brandMapper = brandMapper;
        this.productRepository = productRepository;
        this.productVectorIndexService = productVectorIndexService;
    }

    private static final Map<String, Field> BRAND_FIELDS = Map.of(
            "id", Field.of("id"),
            "name", Field.of("name"),
            "slug", Field.of("slug"),
            "createdAt", Field.of("createdAt", FilterOp.BETWEEN),
            "categoryId", Field.of("category.id"));

    @Transactional
    @Override
    public Page<BrandResponseDto> getAllBrands(BrandSearchRequest request) {
        Specification<Brand> extra = null;
        if (request.getCategoryId() != null) {
            Integer categoryId = request.getCategoryId();
            extra = (root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId);
        }
        return SpecificationBuilder.page(brandRepository, request.getPager(), BRAND_FIELDS,
                Sort.by(Sort.Direction.ASC, "id"), extra).map(brandMapper::toResponseDto);
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
    @Transactional
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

        Brand savedBrand = brandRepository.save(existingBrand);
        productRepository.findIdsByBrandId(savedBrand.getId()).forEach(productVectorIndexService::enqueue);
        return Optional.of(brandMapper.toResponseDto(savedBrand));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!brandRepository.existsById(id)) {
            throw new IllegalArgumentException("Brand not found");
        }
        productRepository.findIdsByBrandId(id).forEach(productVectorIndexService::enqueue);
        brandRepository.deleteById(id);
    }

    @Override
    public Optional<BrandDto> findById(Integer id) {
        return brandRepository.findById(id)
                .map(brandMapper::toDto);
    }

}
