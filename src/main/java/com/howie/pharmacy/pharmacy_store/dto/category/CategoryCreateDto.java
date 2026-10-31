package com.howie.pharmacy.pharmacy_store.dto.category;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateDto {
    private String name;
    private String slug;
    private String imageUrl;
}
