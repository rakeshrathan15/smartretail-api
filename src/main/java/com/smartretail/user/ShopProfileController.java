package com.smartretail.user;

import com.smartretail.address.Address;
import com.smartretail.address.AddressRepository;
import com.smartretail.auth.Identifiers;
import com.smartretail.common.ApiException;
import com.smartretail.security.CurrentUser;
import com.smartretail.shop.ShopRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in shop owner's shop details and map location, asked for after sign-up.
 * Saving publishes {@link ShopLocationChanged}, which places the shop on a delivery route.
 */
@RestController
@RequestMapping("/api/me/shop-profile")
@Transactional
public class ShopProfileController {

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

    private final ProfileRepository profiles;
    private final AddressRepository addresses;
    private final ShopRepository shops;
    private final ApplicationEventPublisher events;

    public ShopProfileController(ProfileRepository profiles, AddressRepository addresses, ShopRepository shops,
            ApplicationEventPublisher events) {
        this.profiles = profiles;
        this.addresses = addresses;
        this.shops = shops;
        this.events = events;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ShopProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return ShopProfileResponse.from(find(CurrentUser.id(jwt)));
    }

    /**
     * Saves the shop details and marks the profile complete. The first time, it also becomes the
     * shop owner's default delivery address, so checkout is ready to use.
     */
    @PutMapping
    public ShopProfileResponse save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ShopProfileRequest r) {
        UUID userId = CurrentUser.id(jwt);
        Profile profile = find(userId);
        String phone = Identifiers.normalizePhone(r.phone());
        String gst = r.gstNumber() == null || r.gstNumber().isBlank() ? null : r.gstNumber().trim().toUpperCase(Locale.ROOT);
        String state = r.state() == null || r.state().isBlank() ? "Telangana" : r.state().trim();
        profile.updateShop(r.shopName().trim(), r.ownerName().trim(), phone, gst, r.address().trim(), r.city().trim(),
                state, r.pincode(), r.lat().setScale(6, RoundingMode.HALF_UP), r.lng().setScale(6, RoundingMode.HALF_UP),
                r.placeId() == null || r.placeId().isBlank() ? null : r.placeId().trim());

        if (addresses.countByUserId(userId) == 0) {
            Address address = new Address(userId);
            address.update("Shop", r.ownerName().trim(), phone, r.address().trim(), null, r.city().trim(), state,
                    r.pincode());
            address.setDefaultAddress(true);
            addresses.save(address);
        }
        // A shop given credit by the admin keeps its name in step with the profile.
        shops.findFirstByOwnerUserIdOrderByCreatedAtAsc(userId).ifPresent(shop -> {
            shop.setName(r.shopName().trim());
            shop.setPhone(phone);
        });
        events.publishEvent(new ShopLocationChanged(userId));
        return ShopProfileResponse.from(profile);
    }

    private Profile find(UUID userId) {
        return profiles.findById(userId).orElseThrow(() -> ApiException.notFound("Profile not found"));
    }
}
