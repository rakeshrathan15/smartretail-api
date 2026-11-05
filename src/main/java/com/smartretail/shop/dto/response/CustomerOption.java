package com.smartretail.shop.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerOption(
        UUID userId, String fullName, String shopName, String phone, String email, BigDecimal creditLimit) {
}
