package com.smartretail.shop.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ShopRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 20) String phone,
        @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be a valid 15-character GSTIN") String gstin,
        @Size(max = 300) String address,
        @Size(max = 80) String city,
        @Pattern(regexp = "^$|^\\d{6}$", message = "must be a 6-digit PIN code") String pincode,
        UUID deliveryRouteId,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") BigDecimal creditLimit) {
}
