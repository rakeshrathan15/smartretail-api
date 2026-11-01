package com.smartretail.picklist.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PickItem(
        UUID productId, String productName, String brand, int unitsPerCase, int cases, int units,
        BigDecimal value) {
}
