package com.howie.pharmacy.pharmacy_store.dto.brand;

import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandSearchRequest extends ListRequest {
    private Integer categoryId;
}
