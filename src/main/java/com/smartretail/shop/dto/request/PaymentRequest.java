package com.smartretail.shop.dto.request;

import com.smartretail.shop.entity.ShopPayment.Method;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull @DecimalMin(value = "0.01", message = "must be more than zero") @DecimalMax("9999999999.99")
        @Digits(integer = 10, fraction = 2, message = "must be in rupees and paise") BigDecimal amount,
        @NotNull Method method,
        @Size(max = 100) String reference) {
}
