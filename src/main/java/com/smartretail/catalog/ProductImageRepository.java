package com.smartretail.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    /** All images of the given products, primary first, for picking each product's thumbnail. */
    @Query("select i from ProductImage i where i.product.id in :ids order by i.primary desc, i.sortOrder asc")
    List<ProductImage> findForProducts(@Param("ids") List<UUID> ids);
}
