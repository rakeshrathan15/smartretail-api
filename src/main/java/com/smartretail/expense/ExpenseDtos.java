package com.smartretail.expense;

import com.smartretail.expense.Expense.Category;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record ExpenseRequest(
            @NotBlank @Size(max = 120) String title,
            @NotNull Category category,
            @NotNull @DecimalMin(value = "0.01", message = "must be more than zero") @DecimalMax("9999999999.99")
            @Digits(integer = 10, fraction = 2, message = "must be in rupees and paise") BigDecimal amount,
            @Size(max = 500) String description,
            @NotNull LocalDate expenseDate) {
    }

    public record ExpenseResponse(
            UUID id, String title, Category category, BigDecimal amount, String description, LocalDate expenseDate) {

        static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(e.getId(), e.getTitle(), e.getCategory(), e.getAmount(), e.getDescription(),
                    e.getExpenseDate());
        }
    }

    public record CategoryTotal(Category category, BigDecimal total) {
    }

    public record ExpenseSummary(BigDecimal total, long count, List<CategoryTotal> byCategory) {
    }
}
