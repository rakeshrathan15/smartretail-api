package com.smartretail.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record SupplierRequest(
            @NotBlank @Size(max = 120) String name,
            @Pattern(regexp = "^$|^[0-9+ -]{6,20}$", message = "must be a phone number") String phone,
            @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = "must be a valid 15-character GSTIN") String gstin,
            @Size(max = 300) String address,
            @Size(max = 500) String notes) {
    }

    public record ActiveRequest(boolean active) {
    }

    public record SupplierResponse(
            UUID id, String name, String phone, String gstin, String address, String notes, boolean active) {

        static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.getId(), s.getName(), s.getPhone(), s.getGstin(), s.getAddress(),
                    s.getNotes(), s.isActive());
        }
    }
}
