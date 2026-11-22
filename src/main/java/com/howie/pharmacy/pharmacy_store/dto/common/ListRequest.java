package com.howie.pharmacy.pharmacy_store.dto.common;

import lombok.Getter;
import lombok.Setter;

/**
 * Base payload of every "getAll..." endpoint: endpoint-specific params plus a
 * {@link PagerRequest}.
 */
@Getter
@Setter
public class ListRequest {
    private PagerRequest pager = new PagerRequest();
}
