package com.smartretail.user.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.security.CurrentUser;
import com.smartretail.user.dto.request.ShopProfileRequest;
import com.smartretail.user.dto.response.ShopProfileResponse;
import com.smartretail.user.service.ShopProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in shop owner's shop details and map location, asked for after sign-up. */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/me/shop-profile")
public class ShopProfileController {

    private final ShopProfileService profiles;

    @GetMapping
    public ApiResponse<ShopProfileResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(profiles.get(CurrentUser.id(jwt)));
    }

    @PutMapping
    public ApiResponse<ShopProfileResponse> save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ShopProfileRequest request) {
        return ApiResponse.ok(profiles.save(CurrentUser.id(jwt), request));
    }
}
