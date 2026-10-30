package com.smartretail.supplier;

import com.smartretail.supplier.SupplierDtos.ActiveRequest;
import com.smartretail.supplier.SupplierDtos.SupplierRequest;
import com.smartretail.supplier.SupplierDtos.SupplierResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RestController
@RequestMapping("/api/admin/suppliers")
public class AdminSupplierController {

    private final SupplierService suppliers;

    public AdminSupplierController(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @GetMapping
    public List<SupplierResponse> list() {
        return suppliers.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
        return suppliers.create(request);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        return suppliers.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public SupplierResponse setActive(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return suppliers.setActive(id, request.active());
    }
}
