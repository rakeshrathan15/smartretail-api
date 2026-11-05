package com.smartretail.order.dto.response;

public record AddressSnapshot(
        String label, String fullName, String phone, String addressLine1, String addressLine2,
        String city, String state, String pincode) {
}
