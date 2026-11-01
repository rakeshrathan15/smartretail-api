package com.smartretail.order.dto.request;

import com.smartretail.order.enums.OrderEnums.Status;
import jakarta.validation.constraints.NotNull;

public record StatusChange(@NotNull Status status) {
}
