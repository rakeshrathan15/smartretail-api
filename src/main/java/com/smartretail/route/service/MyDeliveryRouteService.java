package com.smartretail.route.service;

import com.smartretail.route.dto.response.MyRouteResponse;
import com.smartretail.route.entity.DeliveryRoute;
import com.smartretail.route.repository.DeliveryRouteRepository;
import com.smartretail.user.entity.Profile;
import com.smartretail.user.repository.ProfileRepository;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The route a shop is on and its next delivery day. */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MyDeliveryRouteService {

    private final ProfileRepository profiles;
    private final DeliveryRouteRepository routes;
    private final DeliverySchedule schedule;

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
