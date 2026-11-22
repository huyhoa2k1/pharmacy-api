package com.howie.pharmacy.pharmacy_store.dto.product;

import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchRequest extends ListRequest {
    private Integer categoryId;
    private Boolean onSale;
    private Integer minSold;
}
