package com.smartretail.supplier.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.supplier.dto.request.ActiveRequest;
import com.smartretail.supplier.dto.request.SupplierRequest;
import com.smartretail.supplier.dto.response.SupplierResponse;
import com.smartretail.supplier.service.SupplierService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only (SecurityConfig). */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/suppliers")
public class AdminSupplierController {

    private final SupplierService suppliers;

    @GetMapping
    public ApiResponse<List<SupplierResponse>> list() {
        return ApiResponse.ok(suppliers.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok(suppliers.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierResponse> update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok(suppliers.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<SupplierResponse> setActive(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return ApiResponse.ok(suppliers.setActive(id, request.active()));
    }
}
