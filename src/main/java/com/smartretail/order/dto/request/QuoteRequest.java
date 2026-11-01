package com.smartretail.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The phone sends only products and case counts; every price is worked out here. */
public record QuoteRequest(@NotNull @Size(max = 100) @Valid List<CartItem> items, @Size(max = 40) String couponCode) {
}
