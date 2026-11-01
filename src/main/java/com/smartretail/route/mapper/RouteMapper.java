package com.smartretail.route.mapper;

import com.smartretail.common.mapper.MapStructConfig;
import com.smartretail.route.dto.Point;
import com.smartretail.route.dto.Zone;
import com.smartretail.route.dto.ZoneShape;
import com.smartretail.route.support.DeliveryZone;
import com.smartretail.route.support.GeoPoint;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface RouteMapper {

    Point toPoint(GeoPoint point);

    default Zone toZone(DeliveryZone zone) {
        return switch (zone) {
            case DeliveryZone.Polygon polygon -> new Zone(ZoneShape.POLYGON,
                    polygon.vertices().stream().map(this::toPoint).toList(), null, null);
            case DeliveryZone.Circle circle -> new Zone(ZoneShape.CIRCLE, null, toPoint(circle.center()),
                    circle.radiusKm());
        };
    }
}
