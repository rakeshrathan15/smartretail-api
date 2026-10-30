package com.smartretail.store;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    List<Store> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    @Query(nativeQuery = true, value = "SELECT count(*) FROM delivery_routes WHERE store_id = :storeId")
    long countRoutes(@Param("storeId") UUID storeId);
}
