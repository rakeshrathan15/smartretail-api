package com.smartretail.picklist.dto.response;

import java.math.BigDecimal;

public record Totals(int orders, int uniqueSkus, int cases, int units, BigDecimal itemsValue, BigDecimal revenue) {
}
