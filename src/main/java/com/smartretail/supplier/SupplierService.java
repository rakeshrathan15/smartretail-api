package com.smartretail.supplier;

import com.smartretail.common.ApiException;
import com.smartretail.supplier.SupplierDtos.SupplierRequest;
import com.smartretail.supplier.SupplierDtos.SupplierResponse;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository suppliers;

    public SupplierService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> list() {
        return suppliers.findAllByOrderByNameAsc().stream().map(SupplierResponse::from).toList();
    }

    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = new Supplier(request.name().trim());
        apply(supplier, request);
        return SupplierResponse.from(suppliers.save(supplier));
    }

    public SupplierResponse update(UUID id, SupplierRequest request) {
        Supplier supplier = find(id);
        apply(supplier, request);
        return SupplierResponse.from(supplier);
    }

    public SupplierResponse setActive(UUID id, boolean active) {
        Supplier supplier = find(id);
        supplier.setActive(active);
        return SupplierResponse.from(supplier);
    }

    private Supplier find(UUID id) {
        return suppliers.findById(id).orElseThrow(() -> ApiException.notFound("Supplier not found"));
    }

    private static void apply(Supplier supplier, SupplierRequest r) {
        String gstin = blankToNull(r.gstin());
        supplier.update(r.name().trim(), blankToNull(r.phone()), gstin == null ? null : gstin.toUpperCase(Locale.ROOT),
                blankToNull(r.address()), blankToNull(r.notes()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
