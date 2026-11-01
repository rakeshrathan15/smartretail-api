package com.smartretail.shop.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ShopRow(
        UUID id, UUID ownerUserId, String name, String phone, String gstin, String address, String city,
        String pincode, UUID routeId, String routeName, BigDecimal creditLimit, BigDecimal outstanding,
        boolean overLimit, boolean active) {
}
