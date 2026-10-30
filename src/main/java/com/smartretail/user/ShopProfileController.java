package com.smartretail.user;

import com.smartretail.security.CurrentUser;
import com.smartretail.user.ShopProfileDtos.ShopProfileRequest;
import com.smartretail.user.ShopProfileDtos.ShopProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in shop owner's shop details and map location, asked for after sign-up. */
@RestController
@RequestMapping("/api/me/shop-profile")
public class ShopProfileController {

    private final ShopProfileService profiles;

    public ShopProfileController(ShopProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public ShopProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return profiles.get(CurrentUser.id(jwt));
    }

    @PutMapping
    public ShopProfileResponse save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ShopProfileRequest request) {
        return profiles.save(CurrentUser.id(jwt), request);
    }
}
