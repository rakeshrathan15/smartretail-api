package com.smartretail.expense.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.common.response.PageResponse;
import com.smartretail.expense.dto.request.ExpenseRequest;
import com.smartretail.expense.dto.response.ExpenseResponse;
import com.smartretail.expense.dto.response.ExpenseSummary;
import com.smartretail.expense.entity.Expense.Category;
import com.smartretail.expense.service.ExpenseService;
import com.smartretail.expense.support.ExpenseFilter;
import com.smartretail.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/expenses")
public class AdminExpenseController {

    private final ExpenseService expenses;

    @GetMapping
    public ApiResponse<PageResponse<ExpenseResponse>> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(expenses.list(new ExpenseFilter(from, to, category), page, size));
    }

    @GetMapping("/summary")
    public ApiResponse<ExpenseSummary> summary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Category category) {
        return ApiResponse.ok(expenses.summary(new ExpenseFilter(from, to, category)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(expenses.create(request, CurrentUser.id(jwt)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExpenseResponse> update(@PathVariable UUID id, @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.ok(expenses.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        expenses.delete(id);
    }
}
