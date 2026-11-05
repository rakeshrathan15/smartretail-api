package com.smartretail.picklist.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PickList(
        UUID routeId, String routeName, LocalDate date, List<PickOrder> orders, List<PickItem> items,
        Totals totals) {
}
