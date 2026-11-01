package com.smartretail.route.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.route.dto.request.ActiveRequest;
import com.smartretail.route.dto.request.RouteRequest;
import com.smartretail.route.dto.response.ReassignResponse;
import com.smartretail.route.dto.response.RouteResponse;
import com.smartretail.route.dto.response.ShopPinResponse;
import com.smartretail.route.service.DeliveryRouteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/routes")
public class AdminDeliveryRouteController {

    private final DeliveryRouteService routes;

    @GetMapping
    public ApiResponse<List<RouteResponse>> list() {
        return ApiResponse.ok(routes.list());
    }

    /** Every shop with a map pin and the route it is on, for the routes map. */
    @GetMapping("/shop-pins")
    public ApiResponse<List<ShopPinResponse>> shopPins() {
        return ApiResponse.ok(routes.shopPins());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RouteResponse> create(@Valid @RequestBody RouteRequest request) {
        return ApiResponse.ok(routes.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RouteResponse> update(@PathVariable UUID id, @Valid @RequestBody RouteRequest request) {
        return ApiResponse.ok(routes.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<RouteResponse> setActive(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return ApiResponse.ok(routes.setActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        routes.delete(id);
    }

    /** Places every shop again; routes do this themselves on each change, this is for peace of mind. */
    @PostMapping("/reassign")
    public ApiResponse<ReassignResponse> reassign() {
        return ApiResponse.ok(routes.reassignAll());
    }
}
