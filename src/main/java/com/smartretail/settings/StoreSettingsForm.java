package com.smartretail.settings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Every setting the admin can change, with its type and rules. Stored as key/value rows in
 * {@code store_settings}; all of them are public (shown on the storefront or used at checkout).
 */
public record StoreSettingsForm(
        @NotBlank @Size(max = 80) String storeName,
        @Size(max = 20) String storePhone,
        @Email @Size(max = 120) String storeEmail,
        @Size(max = 300) String storeAddress,
        @Size(max = 20) String whatsappNumber,
        @NotNull Boolean storeOpen,
        @NotNull @DecimalMin("0.00") @DecimalMax("100000.00") BigDecimal deliveryCharge,
        /** 0 means delivery is never free. */
        @NotNull @DecimalMin("0.00") @DecimalMax("10000000.00") BigDecimal freeDeliveryAbove,
        /** 0 means no minimum. */
        @NotNull @DecimalMin("0.00") @DecimalMax("10000000.00") BigDecimal minOrderValue,
        /** Empty turns UPI payment off at checkout. */
        @Pattern(regexp = "^$|^[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}$", message = "must look like name@bank")
        String upiId,
        /** 15-character GSTIN, e.g. 36ABCDE1234F1Z5; saved in capitals. */
        @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be a valid 15-character GSTIN")
        String gstNumber,
        /** 14-digit FSSAI licence number. */
        @Pattern(regexp = "^$|^[0-9]{14}$", message = "must be the 14-digit FSSAI number")
        String fssaiNumber,
        @NotNull Boolean showCertifications) {
}
