package com.smartretail.route;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

    List<DeliveryRoute> findAllByOrderBySortOrderAscNameAsc();

    /** In the order routes are tried when placing a shop: lower sort order wins where areas overlap. */
    List<DeliveryRoute> findAllByActiveTrueOrderBySortOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    @Query(nativeQuery = true, value = "SELECT count(*) FROM orders WHERE delivery_route_id = :routeId")
    long countOrders(@Param("routeId") UUID routeId);

    interface RouteShopCount {
        UUID getRouteId();

        long getShops();
    }

    @Query(nativeQuery = true, value = """
            SELECT delivery_route_id AS routeId, count(*) AS shops
            FROM profiles WHERE delivery_route_id IS NOT NULL
            GROUP BY delivery_route_id
            """)
    List<RouteShopCount> countShopsPerRoute();

    interface ShopPin {
        UUID getUserId();

        String getShopName();

        double getLat();

        double getLng();

        UUID getRouteId();
    }

    /** Every shop with a map pin, to draw on the routes map. */
    @Query(nativeQuery = true, value = """
            SELECT p.user_id AS userId, coalesce(p.shop_name, p.full_name, 'Shop') AS shopName,
                   p.shop_lat AS lat, p.shop_lng AS lng, p.delivery_route_id AS routeId
            FROM profiles p
            WHERE p.shop_lat IS NOT NULL AND p.shop_lng IS NOT NULL
            ORDER BY shopName
            """)
    List<ShopPin> findShopPins();
}
