package com.smartretail.route.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.route.dto.response.MyRouteResponse;
import com.smartretail.route.service.MyDeliveryRouteService;
import com.smartretail.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in shop's delivery route and next delivery day, shown on the profile. */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/me/delivery-route")
public class MyDeliveryRouteController {

    private final MyDeliveryRouteService routes;

    @GetMapping
    public ApiResponse<MyRouteResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(routes.forUser(CurrentUser.id(jwt)));
    }
}
