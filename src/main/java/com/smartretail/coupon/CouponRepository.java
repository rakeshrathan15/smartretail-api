package com.smartretail.coupon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    List<Coupon> findAllByOrderByCreatedAtDesc();

    /**
     * Uses one redemption, unless the limit was reached meanwhile. Returns 1 on success, 0 otherwise.
     * Flushes first so pending changes in the same transaction (such as stock updates) are written,
     * and does not clear the persistence context, which would silently drop them.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Coupon c set c.usedCount = c.usedCount + 1 "
            + "where c.id = :id and (c.usageLimit is null or c.usedCount < c.usageLimit)")
    int redeem(@Param("id") UUID id);

    /** Gives back one redemption, e.g. when the order that used it is cancelled. */
    @Modifying(flushAutomatically = true)
    @Query("update Coupon c set c.usedCount = c.usedCount - 1 where c.code = :code and c.usedCount > 0")
    int release(@Param("code") String code);
}
