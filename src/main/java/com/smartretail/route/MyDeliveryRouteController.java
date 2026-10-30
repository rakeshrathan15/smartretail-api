package com.smartretail.route;

import com.smartretail.route.RouteDtos.MyRouteResponse;
import com.smartretail.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in shop's delivery route and next delivery day, shown on the profile. */
@RestController
@RequestMapping("/api/me/delivery-route")
public class MyDeliveryRouteController {

    private final MyDeliveryRouteService routes;

    public MyDeliveryRouteController(MyDeliveryRouteService routes) {
        this.routes = routes;
    }

    @GetMapping
    public MyRouteResponse get(@AuthenticationPrincipal Jwt jwt) {
        return routes.forUser(CurrentUser.id(jwt));
    }
}
