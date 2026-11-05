package com.smartretail.expense.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseSummary(BigDecimal total, long count, List<CategoryTotal> byCategory) {
}
