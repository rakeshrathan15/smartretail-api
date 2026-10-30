package com.smartretail.store;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class StoreDtos {

    private StoreDtos() {
    }

    public record StoreRequest(
            @NotBlank @Size(max = 80) String name,
            @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = "must be a valid 15-character GSTIN") String gstin,
            @Pattern(regexp = "^$|^[0-9+ -]{6,20}$", message = "must be a phone number") String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 300) String address,
            @Size(max = 80) String city,
            @Pattern(regexp = "^$|^\\d{6}$", message = "must be a 6-digit PIN code") String pincode) {
    }

    public record ActiveRequest(boolean active) {
    }

    public record StoreResponse(
            UUID id, String name, String gstin, String phone, String email, String address, String city,
            String pincode, boolean active, long routeCount) {

        static StoreResponse from(Store store, long routeCount) {
            return new StoreResponse(store.getId(), store.getName(), store.getGstin(), store.getPhone(),
                    store.getEmail(), store.getAddress(), store.getCity(), store.getPincode(), store.isActive(),
                    routeCount);
        }
    }
}
