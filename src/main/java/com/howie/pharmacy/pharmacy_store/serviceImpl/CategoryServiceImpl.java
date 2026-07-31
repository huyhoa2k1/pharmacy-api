package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.howie.pharmacy.pharmacy_store.dto.category.CategoryDto;
import com.howie.pharmacy.pharmacy_store.dto.category.CategoryResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.category.CategoryCreateDto;
import com.howie.pharmacy.pharmacy_store.entity.Category;
import com.howie.pharmacy.pharmacy_store.mapper.CategoryMapper;
import com.howie.pharmacy.pharmacy_store.repository.CategoryRepository;
import com.howie.pharmacy.pharmacy_store.services.CategoryService;

import jakarta.transaction.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public List<CategoryResponseDto> findAll() {
        List<Category> categories = categoryRepository.findAll();
        return categoryMapper.toResponseDtoList(categories);
    }

    @Override
    public Optional<CategoryResponseDto> create(CategoryCreateDto categoryCreateDto) {
        if (categoryRepository.existsByName(categoryCreateDto.getName())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        Category category = categoryMapper.toEntity(categoryCreateDto);
        category.setCreatedAt(java.time.LocalDateTime.now());
        category.setUpdatedAt(java.time.LocalDateTime.now());

        return Optional.of(categoryMapper.toResponseDto(categoryRepository.save(category)));
    }

    @Override
    public Optional<CategoryResponseDto> update(Integer id, CategoryCreateDto categoryCreateDto) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (categoryRepository.existsByName(categoryCreateDto.getName())
                && !existingCategory.getName().equals(categoryCreateDto.getName())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        categoryMapper.updateEntityFromDto(categoryCreateDto, existingCategory);
        existingCategory.setUpdatedAt(java.time.LocalDateTime.now());

        return Optional.of(categoryMapper.toResponseDto(categoryRepository.save(existingCategory)));
    }

    @Override
    public void delete(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category not found");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<CategoryDto> findById(Integer id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toDto);
    }
}
