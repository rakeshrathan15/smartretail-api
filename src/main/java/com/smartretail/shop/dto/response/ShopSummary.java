package com.smartretail.shop.dto.response;

import java.math.BigDecimal;

public record ShopSummary(long totalShops, long activeShops, BigDecimal creditOutstanding, long overLimit) {
}
