package com.smartretail.expense.repository;

import com.smartretail.common.response.PageResponse;
import com.smartretail.expense.dto.response.CategoryTotal;
import com.smartretail.expense.dto.response.ExpenseResponse;
import com.smartretail.expense.entity.Expense.Category;
import com.smartretail.expense.support.ExpenseFilter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of expenses and their totals, for the admin. */
@RequiredArgsConstructor
@Repository
@Transactional(readOnly = true)
public class ExpenseDirectory {

    private final JdbcClient jdbc;

    public PageResponse<ExpenseResponse> find(ExpenseFilter filter, int page, int size) {
        Map<String, Object> params = filter.params();
        long total = jdbc.sql("SELECT count(*) FROM expenses e" + filter.where()).params(params)
                .query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<ExpenseResponse> rows = jdbc.sql("""
                        SELECT e.id, e.title, e.category, e.amount, e.description, e.expense_date FROM expenses e
                        """ + filter.where() + " ORDER BY e.expense_date DESC, e.created_at DESC, e.id LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> new ExpenseResponse(rs.getObject("id", UUID.class), rs.getString("title"),
                        Category.valueOf(rs.getString("category")), rs.getBigDecimal("amount"),
                        rs.getString("description"), rs.getObject("expense_date", LocalDate.class)))
                .list();
        return PageResponse.of(rows, page, size, total);
    }

    public List<CategoryTotal> categoryTotals(ExpenseFilter filter) {
        return jdbc.sql("SELECT e.category, sum(e.amount) AS total FROM expenses e"
                        + filter.where() + " GROUP BY e.category ORDER BY total DESC, e.category")
                .params(filter.params())
                .query((rs, i) -> new CategoryTotal(Category.valueOf(rs.getString("category")), rs.getBigDecimal("total")))
                .list();
    }

    public long count(ExpenseFilter filter) {
        return jdbc.sql("SELECT count(*) FROM expenses e" + filter.where()).params(filter.params())
                .query(Long.class).single();
    }
}
