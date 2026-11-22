package com.howie.pharmacy.pharmacy_store.serviceImpl;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.howie.pharmacy.pharmacy_store.dto.product.ProductCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductDto;
import com.howie.pharmacy.pharmacy_store.dto.common.PagerRequest;
import com.howie.pharmacy.pharmacy_store.dto.common.PagerRequest.FilterOp;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductSearchRequest;
import com.howie.pharmacy.pharmacy_store.entity.Brand;
import com.howie.pharmacy.pharmacy_store.entity.Product;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.ResourceNotFoundException;
import com.howie.pharmacy.pharmacy_store.mapper.ProductMapper;
import com.howie.pharmacy.pharmacy_store.repository.BrandRepository;
import com.howie.pharmacy.pharmacy_store.repository.ProductRepository;
import com.howie.pharmacy.pharmacy_store.services.ProductService;
import com.howie.pharmacy.pharmacy_store.services.ProductVectorIndexService;
import com.howie.pharmacy.pharmacy_store.services.StoreService;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder;
import com.howie.pharmacy.pharmacy_store.utils.SpecificationBuilder.Field;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ProductVectorIndexService productVectorIndexService;
    private final StoreService storeService;

    public ProductServiceImpl(ProductRepository productRepository, BrandRepository brandRepository,
            ProductMapper productMapper, ProductVectorIndexService productVectorIndexService,
            StoreService storeService) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.productMapper = productMapper;
        this.productVectorIndexService = productVectorIndexService;
        this.storeService = storeService;
    }

    private static final Map<String, Field> PRODUCT_FIELDS = Map.ofEntries(
            Map.entry("id", Field.of("id")),
            Map.entry("name", Field.of("name")),
            Map.entry("productName", Field.of("name")),
            Map.entry("description", Field.of("description")),
            Map.entry("price", Field.of("price", FilterOp.BETWEEN)),
            Map.entry("sold", Field.of("sold")),
            Map.entry("amount", Field.of("amount")),
            Map.entry("isSale", Field.of("isSale")),
            Map.entry("createdAt", Field.of("createdAt")),
            Map.entry("brandId", Field.of("brand.id")),
            Map.entry("categoryId", Field.of("brand.category.id")));

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAllProducts(ProductSearchRequest request) {
        PagerRequest pager = request.getPager() != null ? request.getPager() : new PagerRequest();

        Specification<Product> spec = SpecificationBuilder.<Product>build(pager, PRODUCT_FIELDS);
        if (request.getCategoryId() != null) {
            Integer categoryId = request.getCategoryId();
            spec = spec.and((root, q, cb) -> cb.equal(root.get("brand").get("category").get("id"), categoryId));
        }
        if (request.getOnSale() != null) {
            Boolean onSale = request.getOnSale();
            spec = spec.and((root, q, cb) -> cb.equal(root.get("isSale"), onSale));
        }
        if (request.getMinSold() != null) {
            Integer minSold = Math.max(request.getMinSold(), 0);
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("sold"), minSold));
        }

        Pageable pageable = SpecificationBuilder.pageable(pager, PRODUCT_FIELDS, Sort.by(Sort.Direction.ASC, "id"));
        return productRepository.findAll(spec, pageable).map(productMapper::toResponseDto);
    }

    @Override
    @Transactional
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

        Product savedProduct = productRepository.save(product);
        storeService.reconcileStock(savedProduct);
        productVectorIndexService.enqueue(savedProduct.getId());
        return Optional.of(productMapper.toResponseDto(savedProduct));
    }

    @Override
    @Transactional
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

        Product savedProduct = productRepository.save(existingProduct);
        storeService.reconcileStock(savedProduct);
        productVectorIndexService.enqueue(savedProduct.getId());
        return Optional.of(productMapper.toResponseDto(savedProduct));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found");
        }
        productVectorIndexService.enqueue(id);
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
    @Transactional(readOnly = true)
    public List<ProductResponseDto> findByIds(List<Integer> ids) {
        return productMapper.toResponseDtoList(productRepository.findAllById(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> findByPriceRange(BigDecimal minimum, BigDecimal maximum, int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 20);
        return productMapper.toResponseDtoList(productRepository.findProductsByPriceRange(
                minimum, maximum, PageRequest.of(0, boundedLimit)));
    }

    @Override
    @Transactional(readOnly = true)
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
