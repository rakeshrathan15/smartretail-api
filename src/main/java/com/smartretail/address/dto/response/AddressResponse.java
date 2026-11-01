package com.smartretail.address.dto.response;

import java.util.UUID;

public record AddressResponse(
        UUID id, String label, String fullName, String phone, String addressLine1, String addressLine2,
        String city, String state, String pincode, boolean isDefault) {
}
