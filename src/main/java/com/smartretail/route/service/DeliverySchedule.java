package com.smartretail.route.service;

import com.smartretail.common.util.BusinessCalendar;
import com.smartretail.route.entity.DeliveryRoute;
import com.smartretail.route.repository.DeliveryRouteRepository;
import com.smartretail.route.support.DeliveryDays;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** When an order goes out: the route's next delivery day, or the next day for shops off every route. */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class DeliverySchedule {

    private final DeliveryRouteRepository routes;
    private final BusinessCalendar calendar;

    /** The delivery date for an order placed now by a shop on {@code routeId} (may be null). */
    public LocalDate deliveryDateForOrderPlacedToday(UUID routeId) {
        return daysOf(routeId).orElseGet(() -> DeliveryDays.of(null)).nextDeliveryAfter(calendar.today());
    }

    private Optional<DeliveryDays> daysOf(UUID routeId) {
        return routeId == null ? Optional.empty()
                : routes.findById(routeId).filter(DeliveryRoute::isActive).map(DeliveryRoute::getDays);
    }
}
