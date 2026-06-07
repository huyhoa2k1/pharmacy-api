package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;
import com.howie.pharmacy.pharmacy_store.entity.Brand;
import com.howie.pharmacy.pharmacy_store.entity.Product;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.ResourceNotFoundException;
import com.howie.pharmacy.pharmacy_store.mapper.ProductMapper;
import com.howie.pharmacy.pharmacy_store.repository.BrandRepository;
import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;
import com.howie.pharmacy.pharmacy_store.services.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductMapper productMapper;

    @Override
    // @Cacheable(value = "productCache", key = "'allProducts'")
    public List<ProductResponseDto> findAll() {
        List<Product> products = productRepository.findAll();
        return productMapper.toResponseDtoList(products);
    }

    @Override
    // @Cacheable(value = "productCache", key = "#id")
    public Page<ProductResponseDto> getAllProductsByCategory(Integer categoryId, List<Integer> brandIds,
            Double minPrice, Double maxPrice, String sortBy, String sortDirection, int pageNo,
            int pageSize) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(direction, sortBy));

        Page<Product> productsPage = productRepository.getAllProductsByCategory(categoryId, brandIds, minPrice,
                maxPrice, pageable);

        return productsPage.map(productMapper::toResponseDto);
    }

    @Override
    public Optional<ProductResponseDto> create(ProductCreateDto productCreateDto) {
        if (productRepository.existsByName(productCreateDto.getName())) {
            throw new IllegalArgumentException("Product with this name already exists");
        }

        Brand brand = brandRepository.findById(productCreateDto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        Product product = productMapper.toEntity(productCreateDto);
        product.setBrand(brand);
        product.setCreatedAt(java.time.LocalDateTime.now());
        product.setUpdatedAt(java.time.LocalDateTime.now());

        return Optional.of(productMapper.toResponseDto(productRepository.save(product)));
    }

    @Override
    public Optional<ProductResponseDto> update(Integer id, ProductCreateDto productCreateDto) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (productRepository.existsByName(productCreateDto.getName())) {
            throw new IllegalArgumentException("Product with this name already exists");
        }

        Brand brand = brandRepository.findById(productCreateDto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        productMapper.updateEntityFromDto(productCreateDto, existingProduct);
        existingProduct.setBrand(brand);
        existingProduct.setUpdatedAt(java.time.LocalDateTime.now());

        return Optional.of(productMapper.toResponseDto(productRepository.save(existingProduct)));
    }

    @Override
    public void delete(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found");
        }
        productRepository.deleteById(id);
    }

    @Override
    // @Cacheable(value = "productCache", key = "#id")
    public Optional<ProductDto> findById(Integer id) {
        return Optional.ofNullable(productRepository.findById(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found")));
    }

    @Override
    public List<ProductResponseDto> searchProducts(String keyword) {
        List<Product> products = productRepository.searchProducts(keyword);
        return productMapper.toResponseDtoList(products);
    }

    @Override
    @Transactional
    public boolean setSale(List<Integer> productIds, Boolean isSale, Float discount,
            LocalDateTime saleEndTime) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("At least one product ID must be provided");
        }
        if (isSale == null) {
            throw new IllegalArgumentException("isSale must be provided");
        }

        List<Product> products = productRepository.findAllById(productIds);
        if (products.isEmpty() || products.size() != productIds.size()) {
            throw new ResourceNotFoundException("Some products were not found");
        }

        for (Product product : products) {
            product.setIsSale(isSale);
            if (discount != null) {
                product.setDiscount(discount);
            }
            if (Boolean.TRUE.equals(isSale)) {
                product.setSaleEndTime(saleEndTime);
            } else {
                product.setSaleEndTime(null);
            }
            product.setUpdatedAt(LocalDateTime.now());
        }

        productRepository.saveAll(products);
        return true;
    }

}
