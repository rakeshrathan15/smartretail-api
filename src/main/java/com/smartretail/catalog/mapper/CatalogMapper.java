package com.smartretail.catalog.mapper;

import com.smartretail.catalog.dto.response.BannerResponse;
import com.smartretail.catalog.dto.response.CategoryResponse;
import com.smartretail.catalog.dto.response.ImageResponse;
import com.smartretail.catalog.dto.response.SlabResponse;
import com.smartretail.catalog.entity.Banner;
import com.smartretail.catalog.entity.Category;
import com.smartretail.catalog.entity.PriceSlab;
import com.smartretail.catalog.entity.ProductImage;
import com.smartretail.common.mapper.MapStructConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface CatalogMapper {

    CategoryResponse toCategory(Category category);

    ImageResponse toImage(ProductImage image);

    SlabResponse toSlab(PriceSlab slab);

    BannerResponse toBanner(Banner banner);
}
