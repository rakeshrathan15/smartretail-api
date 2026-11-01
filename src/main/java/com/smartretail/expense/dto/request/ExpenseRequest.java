package com.smartretail.expense.dto.request;

import com.smartretail.expense.entity.Expense.Category;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank @Size(max = 120) String title,
        @NotNull Category category,
        @NotNull @DecimalMin(value = "0.01", message = "must be more than zero") @DecimalMax("9999999999.99")
        @Digits(integer = 10, fraction = 2, message = "must be in rupees and paise") BigDecimal amount,
        @Size(max = 500) String description,
        @NotNull LocalDate expenseDate) {
}
