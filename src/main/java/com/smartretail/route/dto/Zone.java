package com.smartretail.route.dto;

import com.smartretail.route.support.DeliveryZone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** A drawn area: POLYGON uses {@code points}; CIRCLE uses {@code center} and {@code radiusKm}. */
public record Zone(
        @NotNull ZoneShape shape,
        @Size(max = DeliveryZone.Polygon.MAX_POINTS) List<@Valid @NotNull Point> points,
        @Valid Point center,
        Double radiusKm) {
}
