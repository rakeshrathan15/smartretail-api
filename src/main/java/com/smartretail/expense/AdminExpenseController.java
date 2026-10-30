package com.smartretail.expense;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.expense.Expense.Category;
import com.smartretail.expense.ExpenseDtos.ExpenseRequest;
import com.smartretail.expense.ExpenseDtos.ExpenseResponse;
import com.smartretail.expense.ExpenseDtos.ExpenseSummary;
import com.smartretail.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/expenses")
public class AdminExpenseController {

    private final ExpenseService expenses;

    public AdminExpenseController(ExpenseService expenses) {
        this.expenses = expenses;
    }

    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return expenses.list(new ExpenseFilter(from, to, category), page, size);
    }

    @GetMapping("/summary")
    public ExpenseSummary summary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Category category) {
        return expenses.summary(new ExpenseFilter(from, to, category));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request, @AuthenticationPrincipal Jwt jwt) {
        return expenses.create(request, CurrentUser.id(jwt));
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable UUID id, @Valid @RequestBody ExpenseRequest request) {
        return expenses.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        expenses.delete(id);
    }
}
