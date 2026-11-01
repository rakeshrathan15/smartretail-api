package com.smartretail.expense.service;

import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.common.response.PageResponse;
import com.smartretail.common.util.BusinessCalendar;
import com.smartretail.expense.dto.request.ExpenseRequest;
import com.smartretail.expense.dto.response.CategoryTotal;
import com.smartretail.expense.dto.response.ExpenseResponse;
import com.smartretail.expense.dto.response.ExpenseSummary;
import com.smartretail.expense.entity.Expense;
import com.smartretail.expense.mapper.ExpenseMapper;
import com.smartretail.expense.repository.ExpenseDirectory;
import com.smartretail.expense.repository.ExpenseRepository;
import com.smartretail.expense.support.ExpenseFilter;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class ExpenseService {

    private final ExpenseMapper expenseMapper;
    private final ExpenseRepository expenses;
    private final ExpenseDirectory directory;
    private final BusinessCalendar calendar;

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(ExpenseFilter filter, int page, int size) {
        return directory.find(filter, page, size);
    }

    @Transactional(readOnly = true)
    public ExpenseSummary summary(ExpenseFilter filter) {
        List<CategoryTotal> byCategory = directory.categoryTotals(filter);
        return new ExpenseSummary(byCategory.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add),
                directory.count(filter), byCategory);
    }

    public ExpenseResponse create(ExpenseRequest request, UUID adminId) {
        Expense expense = new Expense(adminId);
        apply(expense, request);
        return expenseMapper.toResponse(expenses.save(expense));
    }

    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = find(id);
        apply(expense, request);
        return expenseMapper.toResponse(expense);
    }

    public void delete(UUID id) {
        expenses.delete(find(id));
    }

    private void apply(Expense expense, ExpenseRequest r) {
        if (r.expenseDate().isAfter(calendar.today())) {
            throw new ApiException(ErrorCode.EXPENSE_DATE_IN_FUTURE, "The expense date cannot be in the future");
        }
        expense.update(r.title().trim(), r.category(), r.amount(),
                r.description() == null || r.description().isBlank() ? null : r.description().trim(), r.expenseDate());
    }

    private Expense find(UUID id) {
        return expenses.findById(id).orElseThrow(() -> new ApiException(ErrorCode.EXPENSE_NOT_FOUND, "Expense not found"));
    }
}
