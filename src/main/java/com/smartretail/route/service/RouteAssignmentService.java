package com.smartretail.route.service;

import com.smartretail.route.repository.DeliveryRouteRepository;
import com.smartretail.route.support.GeoPoint;
import com.smartretail.route.support.RouteMatcher;
import com.smartretail.shop.repository.ShopRepository;
import com.smartretail.user.entity.Profile;
import com.smartretail.user.event.ShopLocationChanged;
import com.smartretail.user.repository.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places shops on delivery routes from their map pin. A shop is placed when it saves its
 * location, and every shop is placed again whenever a route's area, order or status changes.
 */
@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class RouteAssignmentService {

    public record Summary(int assigned, int unassigned) {
    }

    private final DeliveryRouteRepository routes;
    private final ProfileRepository profiles;
    private final ShopRepository shops;

    /** Runs inside the shop-profile save, so the route is set before the response goes back. */
    @EventListener
    public void onShopLocationChanged(ShopLocationChanged event) {
        profiles.findById(event.userId()).ifPresent(profile -> {
            UUID routeId = profile.getShopLat() == null || profile.getShopLng() == null ? null
                    : matcher().routeFor(pinOf(profile)).orElse(null);
            profile.assignDeliveryRoute(routeId);
            shops.findAllByOwnerUserId(profile.getUserId()).forEach(shop -> shop.assignDeliveryRoute(routeId));
        });
    }

    public Summary reassignAll() {
        RouteMatcher matcher = matcher();
        int assigned = 0;
        int unassigned = 0;
        for (Profile profile : profiles.findAllByShopLatIsNotNullAndShopLngIsNotNull()) {
            UUID routeId = matcher.routeFor(pinOf(profile)).orElse(null);
            profile.assignDeliveryRoute(routeId);
            if (routeId == null) {
                unassigned++;
            } else {
                assigned++;
            }
        }
        profiles.clearRoutesWithoutPin();
        shops.copyRoutesFromProfiles();
        log.info("Placed shops on delivery routes: {} on a route, {} outside every route", assigned, unassigned);
        return new Summary(assigned, unassigned);
    }

    private RouteMatcher matcher() {
        return new RouteMatcher(routes.findAllByActiveTrueOrderBySortOrderAscNameAsc());
    }

    private static GeoPoint pinOf(Profile profile) {
        return new GeoPoint(profile.getShopLat().doubleValue(), profile.getShopLng().doubleValue());
    }
}
