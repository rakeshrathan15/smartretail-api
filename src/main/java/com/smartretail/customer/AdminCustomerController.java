package com.smartretail.customer;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.common.RouteFilter;
import com.smartretail.customer.CustomerDirectory.CustomerRow;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Registered shop owners. Admin-only (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final CustomerDirectory customers;

    public AdminCustomerController(CustomerDirectory customers) {
        this.customers = customers;
    }

    /** {@code route} is {@code all} (default), {@code unassigned} or a route id. */
    @GetMapping
    public PageResponse<CustomerRow> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String route,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return customers.find(q, RouteFilter.parse(route), page, size);
    }
}
