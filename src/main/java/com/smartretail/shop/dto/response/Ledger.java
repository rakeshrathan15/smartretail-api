package com.smartretail.shop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record Ledger(
        ShopRow shop, BigDecimal creditOrdersTotal, BigDecimal paidTotal, List<OpenOrder> openOrders,
        List<PaymentRow> payments) {
}
