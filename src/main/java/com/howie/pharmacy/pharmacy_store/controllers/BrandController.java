package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.howie.pharmacy.pharmacy_store.dto.brand.BrandDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandCreateDto;
import com.howie.pharmacy.pharmacy_store.services.BrandService;

@RestController
@RequestMapping("/api/brands")
public class BrandController {
    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @PostMapping
    public ResponseEntity<BrandResponseDto> createBrand(@RequestBody BrandCreateDto brandCreateDto) {
        Optional<BrandResponseDto> createdBrand = brandService.create(brandCreateDto);
        return createdBrand
                .map(brand -> new ResponseEntity<>(brand, HttpStatus.CREATED))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BrandResponseDto> updateBrand(@PathVariable Integer id, @RequestBody BrandCreateDto brand) {
        Optional<BrandResponseDto> updatedBrand = brandService.update(id, brand);
        return updatedBrand
                .map(brandDto -> new ResponseEntity<>(brandDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<Iterable<BrandResponseDto>> getAllBrands() {
        Iterable<BrandResponseDto> brands = brandService.findAll();
        return new ResponseEntity<>(brands, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandDto> getBrandById(@PathVariable Integer id) {
        Optional<BrandDto> brand = brandService.findById(id);
        return brand
                .map(brandDto -> new ResponseEntity<>(brandDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Iterable<BrandResponseDto>> getBrandsByCategory(@PathVariable Integer categoryId) {
        try {
            Iterable<BrandResponseDto> brands = brandService.getBrandsByCategory(categoryId);
            return new ResponseEntity<>(brands, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
