package com.smartretail.supplier.dto.response;

import java.util.UUID;

public record SupplierResponse(
        UUID id, String name, String phone, String gstin, String address, String notes, boolean active) {
}
