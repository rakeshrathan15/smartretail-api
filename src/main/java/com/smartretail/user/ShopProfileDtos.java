package com.smartretail.user;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class ShopProfileDtos {

    private ShopProfileDtos() {
    }

    /** Latitude and longitude roughly covering India, to catch swapped or empty coordinates. */
    public record ShopProfileRequest(
            @NotBlank @Size(max = 120) String shopName,
            @NotBlank @Size(max = 100) String ownerName,
            @NotBlank @Size(max = 20) String phone,
            @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = "must be a valid 15-character GSTIN") String gstNumber,
            @NotBlank @Size(max = 300) String address,
            @NotBlank @Size(max = 80) String city,
            @Size(max = 80) String state,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit PIN code") String pincode,
            @NotNull @DecimalMin("6.0") @DecimalMax("37.5") BigDecimal lat,
            @NotNull @DecimalMin("68.0") @DecimalMax("98.0") BigDecimal lng,
            @Size(max = 200) String placeId) {
    }

    public record ShopProfileResponse(
            String shopName, String ownerName, String phone, String gstNumber, String address, String city,
            String state, String pincode, BigDecimal lat, BigDecimal lng, boolean complete) {

        static ShopProfileResponse from(Profile p) {
            return new ShopProfileResponse(p.getShopName(), p.getOwnerName() != null ? p.getOwnerName() : p.getFullName(),
                    p.getPhone(), p.getGstNumber(), p.getShopAddress(), p.getShopCity(), p.getShopState(),
                    p.getShopPincode(), p.getShopLat(), p.getShopLng(), p.isShopProfileComplete());
        }
    }
}
