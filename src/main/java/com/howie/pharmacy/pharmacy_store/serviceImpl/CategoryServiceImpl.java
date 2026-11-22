package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;
import com.howie.pharmacy.pharmacy_store.dto.common.PagerRequest.FilterOp;

import com.howie.pharmacy.pharmacy_store.dto.category.CategoryDto;
import com.howie.pharmacy.pharmacy_store.dto.category.CategoryResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.category.CategoryCreateDto;
import com.howie.pharmacy.pharmacy_store.entity.Category;
import com.howie.pharmacy.pharmacy_store.mapper.CategoryMapper;
import com.howie.pharmacy.pharmacy_store.repository.CategoryRepository;
import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;
import com.howie.pharmacy.pharmacy_store.services.CategoryService;
import com.howie.pharmacy.pharmacy_store.services.ProductVectorIndexService;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder.Field;

import jakarta.transaction.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;
    private final ProductVectorIndexService productVectorIndexService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper,
            ProductRepository productRepository, ProductVectorIndexService productVectorIndexService) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.productRepository = productRepository;
        this.productVectorIndexService = productVectorIndexService;
    }

    private static final Map<String, Field> CATEGORY_FIELDS = Map.of(
            "id", Field.of("id"),
            "name", Field.of("name"),
            "slug", Field.of("slug"),
            "createdAt", Field.of("createdAt", FilterOp.BETWEEN));

    @Override
    @Transactional
    public Page<CategoryResponseDto> getAllCategories(ListRequest request) {
        return SpecificationBuilder.page(categoryRepository, request.getPager(), CATEGORY_FIELDS,
                Sort.by(Sort.Direction.ASC, "id"), null).map(categoryMapper::toResponseDto);
    }

    @Override
    public Optional<CategoryResponseDto> create(CategoryCreateDto categoryCreateDto) {
        if (categoryRepository.existsByName(categoryCreateDto.getName())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        Category category = categoryMapper.toEntity(categoryCreateDto);
        if (category.getImageUrl() != null && category.getImageUrl().isBlank()) {
            category.setImageUrl(null);
        }
        category.setCreatedAt(java.time.LocalDateTime.now());
        category.setUpdatedAt(java.time.LocalDateTime.now());

        return Optional.of(categoryMapper.toResponseDto(categoryRepository.save(category)));
    }

    @Override
    @Transactional
    public Optional<CategoryResponseDto> update(Integer id, CategoryCreateDto categoryCreateDto) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (categoryRepository.existsByName(categoryCreateDto.getName())
                && !existingCategory.getName().equals(categoryCreateDto.getName())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        categoryMapper.updateEntityFromDto(categoryCreateDto, existingCategory);
        // Blank imageUrl is the client's signal to clear the image; null keeps the
        // current one.
        if (categoryCreateDto.getImageUrl() != null && categoryCreateDto.getImageUrl().isBlank()) {
            existingCategory.setImageUrl(null);
        }
        existingCategory.setUpdatedAt(java.time.LocalDateTime.now());

        Category savedCategory = categoryRepository.save(existingCategory);
        productRepository.findIdsByCategoryId(savedCategory.getId()).forEach(productVectorIndexService::enqueue);
        return Optional.of(categoryMapper.toResponseDto(savedCategory));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category not found");
        }
        productRepository.findIdsByCategoryId(id).forEach(productVectorIndexService::enqueue);
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<CategoryDto> findById(Integer id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toDto);
    }
}
