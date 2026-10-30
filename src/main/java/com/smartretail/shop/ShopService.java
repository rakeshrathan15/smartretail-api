package com.smartretail.shop;

import com.smartretail.auth.Identifiers;
import com.smartretail.common.ApiException;
import com.smartretail.shop.ShopDtos.CreditRequest;
import com.smartretail.shop.ShopDtos.CustomerOption;
import com.smartretail.shop.ShopDtos.ShopRequest;
import com.smartretail.shop.ShopDtos.ShopRow;
import com.smartretail.user.Profile;
import com.smartretail.user.ProfileRepository;
import com.smartretail.user.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds and edits shops and sets who may order on credit. */
@Service
@Transactional
public class ShopService {

    private final ShopRepository shops;
    private final ShopDirectory directory;
    private final UserRepository users;
    private final ProfileRepository profiles;

    public ShopService(ShopRepository shops, ShopDirectory directory, UserRepository users,
            ProfileRepository profiles) {
        this.shops = shops;
        this.directory = directory;
        this.users = users;
        this.profiles = profiles;
    }

    /** A shop added by hand, for a buyer with no account. */
    public ShopRow create(ShopRequest request) {
        Shop shop = new Shop(null, request.name().trim());
        apply(shop, request);
        checkRoute(request.deliveryRouteId());
        shop.assignDeliveryRoute(request.deliveryRouteId());
        return directory.get(shops.saveAndFlush(shop).getId());
    }

    /** The route of a shop with an account follows its map pin, so it cannot be set here. */
    public ShopRow update(UUID id, ShopRequest request) {
        Shop shop = find(id);
        if (shop.getOwnerUserId() == null) {
            checkRoute(request.deliveryRouteId());
            shop.assignDeliveryRoute(request.deliveryRouteId());
        } else if (!Objects.equals(shop.getDeliveryRouteId(), request.deliveryRouteId())) {
            throw ApiException.badRequest("This shop's route comes from its map pin and cannot be changed here");
        }
        apply(shop, request);
        shops.flush();
        return directory.get(id);
    }

    public ShopRow setActive(UUID id, boolean active) {
        find(id).setActive(active);
        shops.flush();
        return directory.get(id);
    }

    /** Sets the credit limit of a user's shop, creating the shop record if needed. 0 turns credit off. */
    public ShopRow setCredit(CreditRequest request) {
        if (!users.existsById(request.userId())) {
            throw ApiException.notFound("User not found");
        }
        Shop shop = shops.findFirstByOwnerUserIdOrderByCreatedAtAsc(request.userId())
                .orElseGet(() -> shops.save(fromProfile(request.userId())));
        if (request.shopName() != null && !request.shopName().isBlank()) {
            shop.setName(request.shopName().trim());
        }
        shop.setCreditLimit(request.creditLimit());
        shops.flush();
        return directory.get(shop.getId());
    }

    /** Finds shop owners by name, shop name, phone or email, to give them credit. */
    @Transactional(readOnly = true)
    public List<CustomerOption> searchCustomers(String q) {
        String term = q.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "");
        if (term.length() < 2) {
            return List.of();
        }
        return users.searchCustomers("%" + term + "%").stream()
                .map(c -> new CustomerOption(c.getUserId(), c.getFullName(), c.getShopName(), c.getPhone(),
                        c.getEmail(), shops.findFirstByOwnerUserIdOrderByCreatedAtAsc(c.getUserId())
                                .map(Shop::getCreditLimit).orElse(null)))
                .toList();
    }

    private Shop fromProfile(UUID userId) {
        Profile profile = profiles.findById(userId).orElse(null);
        if (profile == null) {
            return new Shop(userId, "Shop");
        }
        Shop shop = new Shop(userId, profile.getShopName() != null ? profile.getShopName()
                : profile.getFullName() != null ? profile.getFullName() : "Shop");
        shop.updateDetails(shop.getName(), profile.getPhone(), profile.getGstNumber(), profile.getShopAddress(),
                profile.getShopCity(), profile.getShopPincode());
        shop.assignDeliveryRoute(profile.getDeliveryRouteId());
        return shop;
    }

    private static void apply(Shop shop, ShopRequest r) {
        shop.updateDetails(r.name().trim(), Identifiers.normalizePhone(r.phone()),
                blankToNull(r.gstin()) == null ? null : r.gstin().trim().toUpperCase(Locale.ROOT),
                blankToNull(r.address()), blankToNull(r.city()), blankToNull(r.pincode()));
        shop.setCreditLimit(r.creditLimit());
    }

    private void checkRoute(UUID routeId) {
        if (routeId != null && !shops.routeExists(routeId)) {
            throw ApiException.notFound("Route not found");
        }
    }

    private Shop find(UUID id) {
        return shops.findById(id).orElseThrow(() -> ApiException.notFound("Shop not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
