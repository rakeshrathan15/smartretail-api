package com.smartretail.inventory.repository;

import com.smartretail.inventory.entity.StockAdjustment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {
}
