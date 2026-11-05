package com.smartretail.order.dto.response;

import java.math.BigDecimal;

public record CreditInfo(BigDecimal limit, BigDecimal outstanding, BigDecimal available) {
}
