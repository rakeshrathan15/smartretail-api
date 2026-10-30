package com.smartretail.user;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    List<Profile> findAllByShopLatIsNotNullAndShopLngIsNotNull();

    /** Shops without a map pin cannot be on a route. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Profile p set p.deliveryRouteId = null where p.shopLat is null or p.shopLng is null")
    int clearRoutesWithoutPin();
}
