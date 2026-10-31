package com.howie.pharmacy.pharmacy_store.dto.category;

import java.time.LocalDate;
import java.util.List;

//import com.howie.pharmacy.pharmacy_store.dto.brand.BrandDto;
import com.howie.pharmacy.pharmacy_store.dto.brand.BrandResponseDto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Integer id;
    private String name;
    private String slug;
    private String imageUrl;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private List<BrandResponseDto> brands;
}
