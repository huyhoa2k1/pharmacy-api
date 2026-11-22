package com.howie.pharmacy.pharmacy_store.dto.common;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagerRequest {
    public static final int MAX_PAGE_SIZE = 100;

    /** 0-based page index. */
    private int pageNo = 0;
    private int pageSize = 10;
    private List<SortOrder> sort = new ArrayList<>();
    private List<String> searchKeys = new ArrayList<>();
    private String searchValue;
    private List<FilterCriteria> filters = new ArrayList<>();

    @Getter
    @Setter
    public static class SortOrder {
        private String field;
        /** ASC or DESC (case-insensitive); defaults to ASC. */
        private String direction = "ASC";
    }

    @Getter
    @Setter
    public static class FilterCriteria {
        private String key;
        /** Optional; when omitted the field's default operator is used. */
        private FilterOp op;
        /** A scalar or a list of scalars. */
        private Object value;
    }

    public enum FilterOp {
        EQ, IN, BETWEEN, GTE, LTE, LIKE
    }
}
