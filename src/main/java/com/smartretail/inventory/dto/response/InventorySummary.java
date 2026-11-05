package com.smartretail.inventory.dto.response;

import java.math.BigDecimal;

/** Products with no cost entered are left out of the two money figures. */
public record InventorySummary(long products, long lowStock, long outOfStock, BigDecimal stockValue,
        BigDecimal potentialProfit) {
}
