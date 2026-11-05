package com.smartretail.expense.support;

import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.expense.entity.Expense.Category;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Which expenses to show. The list and its totals use the same one, so they always agree. */
public record ExpenseFilter(LocalDate from, LocalDate to, Category category) {

    public ExpenseFilter {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE, "The end date is before the start date");
        }
    }

    public String where() {
        return " WHERE TRUE"
                + (from == null ? "" : " AND e.expense_date >= :from")
                + (to == null ? "" : " AND e.expense_date <= :to")
                + (category == null ? "" : " AND e.category = :category");
    }

    public Map<String, Object> params() {
        Map<String, Object> params = new HashMap<>();
        if (from != null) {
            params.put("from", from);
        }
        if (to != null) {
            params.put("to", to);
        }
        if (category != null) {
            params.put("category", category.name());
        }
        return params;
    }
}
