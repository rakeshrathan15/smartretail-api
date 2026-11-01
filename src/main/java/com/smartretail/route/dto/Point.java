package com.smartretail.route.dto;

import com.smartretail.route.support.GeoPoint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Routes are local delivery runs, so their points must be in India (catches swapped lat/lng). */
public record Point(
        @NotNull @DecimalMin("6.0") @DecimalMax("37.5") Double lat,
        @NotNull @DecimalMin("68.0") @DecimalMax("98.0") Double lng) {

    public GeoPoint toGeoPoint() {
        return new GeoPoint(lat, lng);
    }
}
