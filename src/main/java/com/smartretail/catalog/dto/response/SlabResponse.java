package com.smartretail.catalog.dto.response;

import java.math.BigDecimal;

public record SlabResponse(int minQty, Integer maxQty, BigDecimal pricePerCase) {
}
