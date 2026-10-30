package com.smartretail.shop;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRepository extends JpaRepository<Shop, UUID> {

    Optional<Shop> findFirstByOwnerUserIdOrderByCreatedAtAsc(UUID ownerUserId);

    /** Locks the shop row so two credit orders cannot both pass the limit check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shop s where s.id = :id")
    Optional<Shop> lockById(@Param("id") UUID id);

    List<Shop> findAllByOrderByNameAsc();

    List<Shop> findAllByOwnerUserId(UUID ownerUserId);

    /** Credit shops follow their owner's route, which is worked out from the profile's map pin. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE shops s SET delivery_route_id = p.delivery_route_id
            FROM profiles p
            WHERE p.user_id = s.owner_user_id AND s.delivery_route_id IS DISTINCT FROM p.delivery_route_id
            """)
    int copyRoutesFromProfiles();

    /** What the shop owes (see the shop_balances view). Negative if it paid in advance. */
    @Query(nativeQuery = true, value = "SELECT outstanding FROM shop_balances WHERE shop_id = :shopId")
    BigDecimal outstanding(@Param("shopId") UUID shopId);

    @Query(nativeQuery = true, value = "SELECT EXISTS (SELECT 1 FROM delivery_routes WHERE id = :routeId)")
    boolean routeExists(@Param("routeId") UUID routeId);
}
