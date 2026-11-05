package com.smartretail.inventory.dto.response;

import java.util.List;

public record ReceiptDetail(ReceiptRow receipt, String notes, List<ReceiptItemRow> items) {
}
