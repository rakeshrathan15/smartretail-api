package com.smartretail.order.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentReview(@NotNull PaymentDecision decision) {
}
