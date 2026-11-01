package com.smartretail.route.dto.response;

import com.smartretail.route.dto.Zone;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RouteResponse(
        UUID id, String name, String area, List<String> days, UUID storeId, String storeName, String color,
        Zone zone, boolean active, int sortOrder, long shopCount, LocalDate nextDeliveryDate) {
}
