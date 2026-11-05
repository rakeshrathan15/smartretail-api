package com.smartretail.expense.dto.response;

import com.smartretail.expense.entity.Expense.Category;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id, String title, Category category, BigDecimal amount, String description, LocalDate expenseDate) {
}
