package com.smartretail.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

    List<Banner> findAllByActiveTrueOrderBySortOrderAsc();

    List<Banner> findAllByOrderBySortOrderAsc();
}
