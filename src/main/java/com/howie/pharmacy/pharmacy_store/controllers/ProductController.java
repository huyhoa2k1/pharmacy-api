package com.howie.pharmacy.pharmacy_store.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Transformation;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductCreateDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.product.ProductSaleUpdateDto;
import com.howie.pharmacy.pharmacy_store.services.CloudinaryService;
import com.howie.pharmacy.pharmacy_store.services.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload-images")
    public List<String> uploadFileWithResize(@RequestParam("files") List<MultipartFile> files) {
        try {
            // Định nghĩa các kích cỡ bạn muốn tạo ra
            Transformation transformation = new Transformation().width(400).crop("scale");
            return cloudinaryService.uploadImages(files, transformation);

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Trả về danh sách rỗng nếu có lỗi
        }
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductCreateDto productCreateDto) {
        Optional<ProductResponseDto> createdProduct = productService.create(productCreateDto);
        return createdProduct
                .map(product -> new ResponseEntity<>(product, HttpStatus.CREATED))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> products = productService.findAll();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("by-category/{categoryId}")
    public ResponseEntity<Page<ProductResponseDto>> getProductsByCategory(@PathVariable Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<Integer> brandIds,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "price") String sort,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        Page<ProductResponseDto> productsPage = productService.getAllProductsByCategory(
                categoryId,
                brandIds,
                minPrice,
                maxPrice,
                sort,
                sortDirection,
                page,
                size);
        return new ResponseEntity<>(productsPage, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Integer id) {
        Optional<ProductDto> product = productService.findById(id);
        return product
                .map(productDto -> new ResponseEntity<>(productDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDto>> searchProducts(@RequestParam String keyword) {
        List<ProductResponseDto> products = productService.searchProducts(keyword);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @PutMapping("/sale")
    public ResponseEntity<Boolean> setSaleProducts(
            @RequestBody ProductSaleUpdateDto saleUpdateDto) {
        boolean success = productService.setSale(
                saleUpdateDto.getProductIds(),
                saleUpdateDto.getIsSale(),
                saleUpdateDto.getDiscount(),
                saleUpdateDto.getSaleEndTime());
        return new ResponseEntity<>(success, HttpStatus.OK);
    }
}
