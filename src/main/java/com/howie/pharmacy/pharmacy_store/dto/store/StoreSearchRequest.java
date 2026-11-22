package com.howie.pharmacy.pharmacy_store.dto.store;

import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreSearchRequest extends ListRequest {
    private String provinceCode;
    private boolean includeInactive;
}
