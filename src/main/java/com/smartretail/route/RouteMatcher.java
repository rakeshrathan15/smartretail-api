package com.smartretail.route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Finds the route whose area contains a shop. Routes are tried in the given order, so where two
 * areas overlap the first one wins. Routes without an area never match.
 */
final class RouteMatcher {

    private record Candidate(UUID routeId, DeliveryZone zone) {
    }

    private final List<Candidate> candidates;

    RouteMatcher(List<DeliveryRoute> routesInPriorityOrder) {
        this.candidates = routesInPriorityOrder.stream()
                .flatMap(route -> route.zone().map(zone -> new Candidate(route.getId(), zone)).stream())
                .toList();
    }

    Optional<UUID> routeFor(GeoPoint shop) {
        return candidates.stream()
                .filter(candidate -> candidate.zone().contains(shop))
                .map(Candidate::routeId)
                .findFirst();
    }
}
