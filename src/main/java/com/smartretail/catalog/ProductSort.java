package com.smartretail.catalog;

import org.springframework.data.domain.Sort;

public enum ProductSort {
    /** Most reviewed first. Will switch to cases sold once orders exist. */
    POPULAR(Sort.by(Sort.Order.desc("reviewCount"), Sort.Order.asc("name"))),
    PRICE_ASC(Sort.by(Sort.Order.asc("price"), Sort.Order.asc("name"))),
    PRICE_DESC(Sort.by(Sort.Order.desc("price"), Sort.Order.asc("name"))),
    NEWEST(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("name"))),
    NAME(Sort.by(Sort.Order.asc("name")));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }
}
