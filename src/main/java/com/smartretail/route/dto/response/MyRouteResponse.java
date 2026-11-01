package com.smartretail.route.dto.response;

import java.time.LocalDate;
import java.util.List;

/** The signed-in shop's route, for the app and website. */
public record MyRouteResponse(
        boolean assigned, String name, String area, List<String> days, LocalDate nextDeliveryDate) {

    public static final MyRouteResponse NONE = new MyRouteResponse(false, null, null, List.of(), null);
}
