package com.smartretail.user.service;

import com.smartretail.address.entity.Address;
import com.smartretail.address.repository.AddressRepository;
import com.smartretail.auth.support.Identifiers;
import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.shop.repository.ShopRepository;
import com.smartretail.user.dto.request.ShopProfileRequest;
import com.smartretail.user.dto.response.ShopProfileResponse;
import com.smartretail.user.entity.Profile;
import com.smartretail.user.event.ShopLocationChanged;
import com.smartretail.user.mapper.ShopProfileMapper;
import com.smartretail.user.repository.ProfileRepository;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saving publishes {@link ShopLocationChanged}, which places the shop on a delivery route. */
@RequiredArgsConstructor
@Service
@Transactional
public class ShopProfileService {

    private final ShopProfileMapper shopProfileMapper;
    private final ProfileRepository profiles;
    private final AddressRepository addresses;
    private final ShopRepository shops;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public ShopProfileResponse get(UUID userId) {
        return shopProfileMapper.toResponse(find(userId));
    }

    /**
     * Saves the shop details and marks the profile complete. The first time, it also becomes the
     * shop owner's default delivery address, so checkout is ready to use.
     */
    public ShopProfileResponse save(UUID userId, ShopProfileRequest r) {
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
        // A shop given credit by the admin keeps its details in step with the profile.
        shops.findFirstByOwnerUserIdOrderByCreatedAtAsc(userId).ifPresent(shop -> shop.updateDetails(r.shopName().trim(),
                phone, gst, r.address().trim(), r.city().trim(), r.pincode()));
        events.publishEvent(new ShopLocationChanged(userId));
        return shopProfileMapper.toResponse(profile);
    }

    private Profile find(UUID userId) {
        return profiles.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND, "Profile not found"));
    }
}
