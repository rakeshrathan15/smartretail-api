package com.smartretail.route;

import com.smartretail.route.RouteDtos.ActiveRequest;
import com.smartretail.route.RouteDtos.ReassignResponse;
import com.smartretail.route.RouteDtos.RouteRequest;
import com.smartretail.route.RouteDtos.RouteResponse;
import com.smartretail.route.RouteDtos.ShopPinResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Delivery routes and the shops on them. Admin-only (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/routes")
public class AdminDeliveryRouteController {

    private final DeliveryRouteService routes;

    public AdminDeliveryRouteController(DeliveryRouteService routes) {
        this.routes = routes;
    }

    @GetMapping
    public List<RouteResponse> list() {
        return routes.list();
    }

    /** Every shop with a map pin and the route it is on, for the routes map. */
    @GetMapping("/shop-pins")
    public List<ShopPinResponse> shopPins() {
        return routes.shopPins();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse create(@Valid @RequestBody RouteRequest request) {
        return routes.create(request);
    }

    @PutMapping("/{id}")
    public RouteResponse update(@PathVariable UUID id, @Valid @RequestBody RouteRequest request) {
        return routes.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public RouteResponse setActive(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return routes.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        routes.delete(id);
    }

    /** Places every shop again; routes do this themselves on each change, this is for peace of mind. */
    @PostMapping("/reassign")
    public ReassignResponse reassign() {
        return routes.reassignAll();
    }
}
