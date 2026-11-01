package com.smartretail.expense.dto.response;

import com.smartretail.expense.entity.Expense.Category;
import java.math.BigDecimal;

public record CategoryTotal(Category category, BigDecimal total) {
}
