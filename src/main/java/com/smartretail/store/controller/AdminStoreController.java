package com.smartretail.store.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.store.dto.request.ActiveRequest;
import com.smartretail.store.dto.request.StoreRequest;
import com.smartretail.store.dto.response.StoreResponse;
import com.smartretail.store.service.StoreService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Warehouses and branches. Admin-only (SecurityConfig). */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/stores")
public class AdminStoreController {

    private final StoreService stores;

    @GetMapping
    public ApiResponse<List<StoreResponse>> list() {
        return ApiResponse.ok(stores.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreResponse> create(@Valid @RequestBody StoreRequest request) {
        return ApiResponse.ok(stores.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StoreResponse> update(@PathVariable UUID id, @Valid @RequestBody StoreRequest request) {
        return ApiResponse.ok(stores.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<StoreResponse> setActive(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return ApiResponse.ok(stores.setActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        stores.delete(id);
    }
}
