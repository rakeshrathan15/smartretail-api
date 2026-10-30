package com.smartretail.expense;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.common.ApiException;
import com.smartretail.common.BusinessCalendar;
import com.smartretail.expense.Expense.Category;
import com.smartretail.expense.ExpenseDtos.CategoryTotal;
import com.smartretail.expense.ExpenseDtos.ExpenseRequest;
import com.smartretail.expense.ExpenseDtos.ExpenseResponse;
import com.smartretail.expense.ExpenseDtos.ExpenseSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final JdbcClient jdbc;
    private final BusinessCalendar calendar;

    public ExpenseService(ExpenseRepository expenses, JdbcClient jdbc, BusinessCalendar calendar) {
        this.expenses = expenses;
        this.jdbc = jdbc;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(ExpenseFilter filter, int page, int size) {
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

    @Transactional(readOnly = true)
    public ExpenseSummary summary(ExpenseFilter filter) {
        List<CategoryTotal> byCategory = jdbc.sql("SELECT e.category, sum(e.amount) AS total FROM expenses e"
                        + filter.where() + " GROUP BY e.category ORDER BY total DESC, e.category")
                .params(filter.params())
                .query((rs, i) -> new CategoryTotal(Category.valueOf(rs.getString("category")), rs.getBigDecimal("total")))
                .list();
        long count = jdbc.sql("SELECT count(*) FROM expenses e" + filter.where()).params(filter.params())
                .query(Long.class).single();
        return new ExpenseSummary(byCategory.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add),
                count, byCategory);
    }

    public ExpenseResponse create(ExpenseRequest request, UUID adminId) {
        Expense expense = new Expense(adminId);
        apply(expense, request);
        return ExpenseResponse.from(expenses.save(expense));
    }

    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = find(id);
        apply(expense, request);
        return ExpenseResponse.from(expense);
    }

    public void delete(UUID id) {
        expenses.delete(find(id));
    }

    private void apply(Expense expense, ExpenseRequest r) {
        if (r.expenseDate().isAfter(calendar.today())) {
            throw ApiException.badRequest("The expense date cannot be in the future");
        }
        expense.update(r.title().trim(), r.category(), r.amount(),
                r.description() == null || r.description().isBlank() ? null : r.description().trim(), r.expenseDate());
    }

    private Expense find(UUID id) {
        return expenses.findById(id).orElseThrow(() -> ApiException.notFound("Expense not found"));
    }
}
