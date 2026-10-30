package com.smartretail.route;

import com.smartretail.route.RouteDtos.MyRouteResponse;
import com.smartretail.security.CurrentUser;
import com.smartretail.user.Profile;
import com.smartretail.user.ProfileRepository;
import java.util.Arrays;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in shop's delivery route and next delivery day, shown on the profile. */
@RestController
@RequestMapping("/api/me/delivery-route")
@Transactional(readOnly = true)
public class MyDeliveryRouteController {

    private final ProfileRepository profiles;
    private final DeliveryRouteRepository routes;
    private final DeliverySchedule schedule;

    public MyDeliveryRouteController(ProfileRepository profiles, DeliveryRouteRepository routes,
            DeliverySchedule schedule) {
        this.profiles = profiles;
        this.routes = routes;
        this.schedule = schedule;
    }

    @GetMapping
    public MyRouteResponse get(@AuthenticationPrincipal Jwt jwt) {
        return profiles.findById(CurrentUser.id(jwt))
                .map(Profile::getDeliveryRouteId)
                .flatMap(routes::findById)
                .filter(DeliveryRoute::isActive)
                .map(route -> new MyRouteResponse(true, route.getName(), route.getArea(),
                        Arrays.asList(route.getDays().toCodes()),
                        schedule.deliveryDateForOrderPlacedToday(route.getId())))
                .orElse(MyRouteResponse.NONE);
    }
}
