package com.smartretail.store;

import com.smartretail.store.StoreDtos.ActiveRequest;
import com.smartretail.store.StoreDtos.StoreRequest;
import com.smartretail.store.StoreDtos.StoreResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RestController
@RequestMapping("/api/admin/stores")
public class AdminStoreController {

    private final StoreService stores;

    public AdminStoreController(StoreService stores) {
        this.stores = stores;
    }

    @GetMapping
    public List<StoreResponse> list() {
        return stores.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse create(@Valid @RequestBody StoreRequest request) {
        return stores.create(request);
    }

    @PutMapping("/{id}")
    public StoreResponse update(@PathVariable UUID id, @Valid @RequestBody StoreRequest request) {
        return stores.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public StoreResponse setActive(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return stores.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        stores.delete(id);
    }
}
