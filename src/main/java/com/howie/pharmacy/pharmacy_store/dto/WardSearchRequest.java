package com.howie.pharmacy.pharmacy_store.dto;

import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WardSearchRequest extends ListRequest {
    private String provinceCode;
}
