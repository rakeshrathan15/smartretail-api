package com.smartretail.route.dto.response;

import java.util.UUID;

public record ShopPinResponse(UUID userId, String shopName, double lat, double lng, UUID routeId) {
}
