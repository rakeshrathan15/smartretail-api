package com.smartretail.store.dto.response;

import java.util.UUID;

public record StoreResponse(
        UUID id, String name, String gstin, String phone, String email, String address, String city,
        String pincode, boolean active, long routeCount) {
}
