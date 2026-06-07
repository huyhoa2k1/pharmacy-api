package com.howie.pharmacy.pharmacy_store.dto.product;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSaleUpdateDto {
    private List<Integer> productIds;
    private Boolean isSale;
    private Float discount;
    private LocalDateTime saleEndTime;
}
