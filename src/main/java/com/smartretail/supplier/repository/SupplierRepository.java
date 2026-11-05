package com.smartretail.supplier.repository;

import com.smartretail.supplier.entity.Supplier;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findAllByOrderByNameAsc();
}
