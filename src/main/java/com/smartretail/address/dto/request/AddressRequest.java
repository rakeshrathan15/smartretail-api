package com.smartretail.address.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
