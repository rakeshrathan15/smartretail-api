package com.smartretail.shop;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRepository extends JpaRepository<Shop, UUID> {

    Optional<Shop> findFirstByOwnerUserIdOrderByCreatedAtAsc(UUID ownerUserId);

    /** Locks the shop row so two credit orders cannot both pass the limit check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shop s where s.id = :id")
    Optional<Shop> lockById(@Param("id") UUID id);

    List<Shop> findAllByOrderByNameAsc();

    /**
     * What the shop owes: credit orders not cancelled or returned, minus repayments received.
     * Can be negative if the shop paid in advance.
     */
    @Query(nativeQuery = true, value = """
            SELECT COALESCE((SELECT SUM(o.total) FROM orders o
                             WHERE o.shop_id = :shopId AND o.payment_terms = 'CREDIT'
                               AND o.status NOT IN ('CANCELLED', 'RETURNED')), 0)
                 - COALESCE((SELECT SUM(p.amount) FROM shop_payments p WHERE p.shop_id = :shopId), 0)
            """)
    BigDecimal outstanding(@Param("shopId") UUID shopId);
}
