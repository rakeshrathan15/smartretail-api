package com.smartretail.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AddressDtos {

    private AddressDtos() {
    }

    public record AddressRequest(
            @Size(max = 30) String label,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @NotBlank @Size(max = 80) String city,
            @Size(max = 80) String state,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit PIN code") String pincode,
            Boolean makeDefault) {
    }

    public record AddressResponse(
            UUID id, String label, String fullName, String phone, String addressLine1, String addressLine2,
            String city, String state, String pincode, boolean isDefault) {

        static AddressResponse from(Address a) {
            return new AddressResponse(a.getId(), a.getLabel(), a.getFullName(), a.getPhone(), a.getAddressLine1(),
                    a.getAddressLine2(), a.getCity(), a.getState(), a.getPincode(), a.isDefaultAddress());
        }
    }
}
