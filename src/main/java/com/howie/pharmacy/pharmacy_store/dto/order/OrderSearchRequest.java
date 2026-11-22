package com.howie.pharmacy.pharmacy_store.dto.order;

import com.howie.pharmacy.pharmacy_store.dto.common.ListRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSearchRequest extends ListRequest {
    private Integer userId;
}
