package com.smartretail.route;

import com.smartretail.common.ApiException;
import com.smartretail.common.BusinessCalendar;
import com.smartretail.route.DeliveryRouteRepository.RouteShopCount;
import com.smartretail.route.RouteDtos.ReassignResponse;
import com.smartretail.route.RouteDtos.RouteRequest;
import com.smartretail.route.RouteDtos.RouteResponse;
import com.smartretail.route.RouteDtos.ShopPinResponse;
import com.smartretail.route.RouteDtos.Zone;
import com.smartretail.store.Store;
import com.smartretail.store.StoreService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivery routes for the admin. Every change that can move a shop to another route (area,
 * priority, on/off, delete) places all shops again, so the routes map and pick-lists never go stale.
 */
@Service
@Transactional
public class DeliveryRouteService {

    private final DeliveryRouteRepository routes;
    private final StoreService stores;
    private final RouteAssignmentService assignment;
    private final BusinessCalendar calendar;

    public DeliveryRouteService(DeliveryRouteRepository routes, StoreService stores, RouteAssignmentService assignment,
            BusinessCalendar calendar) {
        this.routes = routes;
        this.stores = stores;
        this.assignment = assignment;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> list() {
        Map<UUID, Long> shopCounts = routes.countShopsPerRoute().stream()
                .collect(Collectors.toMap(RouteShopCount::getRouteId, RouteShopCount::getShops));
        Map<UUID, String> storeNames = stores.namesById();
        return routes.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(route -> toResponse(route, storeNames.get(route.getStoreId()),
                        shopCounts.getOrDefault(route.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShopPinResponse> shopPins() {
        return routes.findShopPins().stream()
                .map(p -> new ShopPinResponse(p.getUserId(), p.getShopName(), p.getLat(), p.getLng(), p.getRouteId()))
                .toList();
    }

    public RouteResponse create(RouteRequest request) {
        String name = request.name().trim();
        if (routes.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A route named " + name + " already exists");
        }
        DeliveryRoute route = new DeliveryRoute(name);
        apply(route, request);
        routes.saveAndFlush(route);
        assignment.reassignAll();
        return single(route);
    }

    public RouteResponse update(UUID id, RouteRequest request) {
        DeliveryRoute route = find(id);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(route.getName()) && routes.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A route named " + name + " already exists");
        }
        apply(route, request);
        routes.flush();
        assignment.reassignAll();
        return single(find(id));
    }

    public RouteResponse setActive(UUID id, boolean active) {
        DeliveryRoute route = find(id);
        route.setActive(active);
        routes.flush();
        assignment.reassignAll();
        return single(find(id));
    }

    /** Routes with orders stay for the order history and old pick-lists; turn those off instead. */
    public void delete(UUID id) {
        DeliveryRoute route = find(id);
        long orders = routes.countOrders(id);
        if (orders > 0) {
            throw ApiException.conflict(route.getName() + " has " + orders + (orders == 1 ? " order" : " orders")
                    + "; turn it off instead of deleting");
        }
        routes.delete(route);
        routes.flush();
        assignment.reassignAll();
    }

    public ReassignResponse reassignAll() {
        RouteAssignmentService.Summary summary = assignment.reassignAll();
        return new ReassignResponse(summary.assigned(), summary.unassigned());
    }

    private void apply(DeliveryRoute route, RouteRequest r) {
        UUID storeId = r.storeId();
        if (storeId != null) {
            stores.find(storeId);
        }
        DeliveryDays days = DeliveryDays.fromCodes(r.days().toArray(String[]::new));
        route.update(r.name().trim(), blankToNull(r.area()), days, storeId,
                blankToNull(r.color()) == null ? null : r.color().toUpperCase(Locale.ROOT),
                r.sortOrder() == null ? route.getSortOrder() : r.sortOrder());
        route.setZone(r.zone() == null ? null : toZone(r.zone()));
        if (r.active() != null) {
            route.setActive(r.active());
        }
    }

    /** Turns the drawn area into a zone, with a 400 that says what is wrong with it. */
    static DeliveryZone toZone(Zone zone) {
        try {
            return switch (zone.shape()) {
                case POLYGON -> new DeliveryZone.Polygon(zone.points() == null ? List.of()
                        : zone.points().stream().map(RouteDtos.Point::toGeoPoint).toList());
                case CIRCLE -> new DeliveryZone.Circle(zone.center() == null ? null : zone.center().toGeoPoint(),
                        zone.radiusKm() == null ? Double.NaN : zone.radiusKm());
            };
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
    }

    private RouteResponse single(DeliveryRoute route) {
        String storeName = route.getStoreId() == null ? null : stores.find(route.getStoreId()).getName();
        long shops = routes.countShopsPerRoute().stream()
                .filter(c -> c.getRouteId().equals(route.getId()))
                .mapToLong(RouteShopCount::getShops)
                .sum();
        return toResponse(route, storeName, shops);
    }

    private RouteResponse toResponse(DeliveryRoute route, String storeName, long shopCount) {
        DeliveryDays days = route.getDays();
        LocalDate next = days.isEmpty() ? null : days.nextDeliveryAfter(calendar.today().minusDays(1));
        return new RouteResponse(route.getId(), route.getName(), route.getArea(),
                Arrays.asList(days.toCodes()), route.getStoreId(), storeName, route.getColor(),
                route.zone().map(Zone::from).orElse(null), route.isActive(), route.getSortOrder(), shopCount, next);
    }

    private DeliveryRoute find(UUID id) {
        return routes.findById(id).orElseThrow(() -> ApiException.notFound("Route not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
