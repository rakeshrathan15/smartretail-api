package com.smartretail.route;

import com.smartretail.route.RouteDtos.MyRouteResponse;
import com.smartretail.user.Profile;
import com.smartretail.user.ProfileRepository;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The route a shop is on and its next delivery day. */
@Service
@Transactional(readOnly = true)
public class MyDeliveryRouteService {

    private final ProfileRepository profiles;
    private final DeliveryRouteRepository routes;
    private final DeliverySchedule schedule;

    public MyDeliveryRouteService(ProfileRepository profiles, DeliveryRouteRepository routes,
            DeliverySchedule schedule) {
        this.profiles = profiles;
        this.routes = routes;
        this.schedule = schedule;
    }

    public MyRouteResponse forUser(UUID userId) {
        return profiles.findById(userId)
                .map(Profile::getDeliveryRouteId)
                .flatMap(routes::findById)
                .filter(DeliveryRoute::isActive)
                .map(route -> new MyRouteResponse(true, route.getName(), route.getArea(),
                        Arrays.asList(route.getDays().toCodes()),
                        schedule.deliveryDateForOrderPlacedToday(route.getId())))
                .orElse(MyRouteResponse.NONE);
    }
}
