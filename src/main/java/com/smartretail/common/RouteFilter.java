package com.smartretail.common;

import java.util.Optional;
import java.util.UUID;

/**
 * A "which route" filter from a query parameter: every route ({@code all} or empty), shops or
 * orders on no route ({@code unassigned}), or one route by id.
 */
public sealed interface RouteFilter permits RouteFilter.Any, RouteFilter.Unassigned, RouteFilter.Route {

    record Any() implements RouteFilter {
    }

    record Unassigned() implements RouteFilter {
    }

    record Route(UUID id) implements RouteFilter {
    }

    static RouteFilter parse(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("all")) {
            return new Any();
        }
        if (value.equalsIgnoreCase("unassigned")) {
            return new Unassigned();
        }
        try {
            return new Route(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("route must be all, unassigned or a route id");
        }
    }

    default Optional<UUID> routeId() {
        return this instanceof Route route ? Optional.of(route.id()) : Optional.empty();
    }
}
