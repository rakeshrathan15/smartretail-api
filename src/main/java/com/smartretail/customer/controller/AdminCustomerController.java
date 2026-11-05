package com.smartretail.customer.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.common.response.PageResponse;
import com.smartretail.common.util.RouteFilter;
import com.smartretail.customer.repository.CustomerDirectory.CustomerRow;
import com.smartretail.customer.repository.CustomerDirectory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Registered shop owners. Admin-only (SecurityConfig). */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final CustomerDirectory customers;

    /** {@code route} is {@code all} (default), {@code unassigned} or a route id. */
    @GetMapping
    public ApiResponse<PageResponse<CustomerRow>> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String route,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(customers.find(q, RouteFilter.parse(route), page, size));
    }
}
