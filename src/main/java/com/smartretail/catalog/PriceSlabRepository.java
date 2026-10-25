package com.smartretail.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceSlabRepository extends JpaRepository<PriceSlab, UUID> {

    @Query("select s.product.id as productId, min(s.pricePerCase) as lowest from PriceSlab s "
            + "where s.product.id in :ids group by s.product.id")
    List<LowestSlabPrice> findLowestPrices(@Param("ids") List<UUID> ids);

    interface LowestSlabPrice {
        UUID getProductId();

        BigDecimal getLowest();
    }
}
