package com.smartretail.user.dto.response;

import java.math.BigDecimal;

public record ShopProfileResponse(
        String shopName, String ownerName, String phone, String gstNumber, String address, String city,
        String state, String pincode, BigDecimal lat, BigDecimal lng, boolean complete) {
}
