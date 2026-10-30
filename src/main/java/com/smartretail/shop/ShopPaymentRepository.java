package com.smartretail.shop;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopPaymentRepository extends JpaRepository<ShopPayment, UUID> {

    Optional<ShopPayment> findByIdAndShopId(UUID id, UUID shopId);
}
