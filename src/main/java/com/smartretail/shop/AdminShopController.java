package com.smartretail.shop;

import com.smartretail.common.ApiException;
import com.smartretail.user.Profile;
import com.smartretail.user.ProfileRepository;
import com.smartretail.user.User;
import com.smartretail.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credit limits for shop owners. Admin-only (SecurityConfig). The full shops and repayments
 * screens come with the credit-ledger feature; this is what checkout needs.
 */
@RestController
@RequestMapping("/api/admin/shops")
@Transactional
public class AdminShopController {

    public record CreditRequest(
            @NotNull UUID userId,
            @Size(max = 120) String shopName,
            @NotNull @DecimalMin("0.00") BigDecimal creditLimit) {
    }

    public record ShopCreditResponse(
            UUID id, UUID ownerUserId, String name, String ownerPhone, BigDecimal creditLimit, BigDecimal outstanding,
            boolean active) {
    }

    public record CustomerOption(
            UUID userId, String fullName, String shopName, String phone, String email, BigDecimal creditLimit) {
    }

    private final ShopRepository shops;
    private final UserRepository users;
    private final ProfileRepository profiles;

    public AdminShopController(ShopRepository shops, UserRepository users, ProfileRepository profiles) {
        this.shops = shops;
        this.users = users;
        this.profiles = profiles;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ShopCreditResponse> list() {
        return shops.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    /** Finds shop owners by name, shop name, phone or email, to give them credit. */
    @GetMapping("/customers")
    @Transactional(readOnly = true)
    public List<CustomerOption> searchCustomers(@RequestParam(defaultValue = "") String q) {
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

    /** Sets the credit limit of a user's shop, creating the shop record if needed. 0 turns credit off. */
    @PutMapping("/credit")
    public ShopCreditResponse setCredit(@Valid @RequestBody CreditRequest request) {
        if (!users.existsById(request.userId())) {
            throw ApiException.notFound("User not found");
        }
        Shop shop = shops.findFirstByOwnerUserIdOrderByCreatedAtAsc(request.userId()).orElseGet(() -> {
            Profile profile = profiles.findById(request.userId()).orElse(null);
            String name = profile != null && profile.getShopName() != null ? profile.getShopName()
                    : profile != null && profile.getFullName() != null ? profile.getFullName() : "Shop";
            return shops.save(new Shop(request.userId(), name));
        });
        if (request.shopName() != null && !request.shopName().isBlank()) {
            shop.setName(request.shopName().trim());
        }
        shop.setCreditLimit(request.creditLimit());
        return toResponse(shop);
    }

    private ShopCreditResponse toResponse(Shop shop) {
        String phone = shop.getOwnerUserId() == null ? null
                : users.findById(shop.getOwnerUserId()).map(User::getPhone).orElse(null);
        return new ShopCreditResponse(shop.getId(), shop.getOwnerUserId(), shop.getName(), phone, shop.getCreditLimit(),
                shop.getId() == null ? BigDecimal.ZERO : shops.outstanding(shop.getId()), shop.isActive());
    }
}
