package com.smartretail.supplier.service;

import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.supplier.dto.request.SupplierRequest;
import com.smartretail.supplier.dto.response.SupplierResponse;
import com.smartretail.supplier.entity.Supplier;
import com.smartretail.supplier.mapper.SupplierMapper;
import com.smartretail.supplier.repository.SupplierRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final SupplierRepository suppliers;

    @Transactional(readOnly = true)
    public List<SupplierResponse> list() {
        return suppliers.findAllByOrderByNameAsc().stream().map(supplierMapper::toResponse).toList();
    }

    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = new Supplier(request.name().trim());
        apply(supplier, request);
        return supplierMapper.toResponse(suppliers.save(supplier));
    }

    public SupplierResponse update(UUID id, SupplierRequest request) {
        Supplier supplier = find(id);
        apply(supplier, request);
        return supplierMapper.toResponse(supplier);
    }

    public SupplierResponse setActive(UUID id, boolean active) {
        Supplier supplier = find(id);
        supplier.setActive(active);
        return supplierMapper.toResponse(supplier);
    }

    private Supplier find(UUID id) {
        return suppliers.findById(id).orElseThrow(() -> new ApiException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier not found"));
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
